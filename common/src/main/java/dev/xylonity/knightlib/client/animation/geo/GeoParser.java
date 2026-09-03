package dev.xylonity.knightlib.client.animation.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.animation.internal.GeoAnimationParser;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.client.animation.model.GeoCube;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads geo geometry and bakes it into KnightLib's own bone/cube format. This is where Bedrock pivots, UVs, mirror flags and
 * coordinate conventions are translated to Minecraft's render space.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/object/GeometryTree.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/object/BakedModelFactory.java
 */
public final class GeoParser {

    private static final Vector3f[] BASE_NORMALS = {
            new Vector3f(0, 0, -1),
            new Vector3f(1, 0, 0),
            new Vector3f(0, 0, 1),
            new Vector3f(-1, 0, 0),
            new Vector3f(0, 1, 0),
            new Vector3f(0, -1, 0)
    };

    private static final Set<String> WARNED = new HashSet<>();

    private GeoParser() {
        ;;
    }

    public static GeoModelDefinition parseModel(JsonObject root) {
        if (!root.has("minecraft:geometry") || !root.get("minecraft:geometry").isJsonArray() || root.getAsJsonArray("minecraft:geometry").isEmpty()) {
            throw new IllegalArgumentException("[KnightLib] Geometry file has no minecraft:geometry entry");
        }

        final JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        final JsonObject description = geometry.getAsJsonObject("description");
        final int textureWidth = description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
        final int textureHeight = description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("[KnightLib] Geometry texture dimensions must be positive");
        }

        final List<GeoModelDefinition.BoneDefinition> bones = new ArrayList<>();
        final Map<String, JsonObject> rawBones = new LinkedHashMap<>();
        final Map<String, Vector3f> absolutePivots = new LinkedHashMap<>();

        if (geometry.has("bones")) {
            for (final JsonElement element : geometry.getAsJsonArray("bones")) {
                final JsonObject bone = element.getAsJsonObject();
                final String name = bone.get("name").getAsString();
                if (name.isBlank() || rawBones.putIfAbsent(name, bone) != null) {
                    throw new IllegalArgumentException("[KnightLib] Duplicate or empty bone name '" + name + "'");
                }

                absolutePivots.put(name, parseVector(bone, "pivot").mul(-1, 1, 1));
            }

            validateHierarchy(rawBones);

            for (final Map.Entry<String, JsonObject> raw : rawBones.entrySet()) {
                final String name = raw.getKey();
                final JsonObject bone = raw.getValue();
                final String parent = parentName(bone);

                final Vector3f pivot = absolutePivots.get(name);
                final Vector3f rotation = parseVector(bone, "rotation").mul(-1, -1, 1);
                final boolean defaultMirror = bone.has("mirror") && bone.get("mirror").getAsBoolean();
                final float defaultInflate = bone.has("inflate") ? bone.get("inflate").getAsFloat() : 0f;
                if (!Float.isFinite(defaultInflate)) {
                    throw new IllegalArgumentException("[KnightLib] Bone inflate must be finite");
                }

                final List<GeoCube> cubes = new ArrayList<>();
                if (bone.has("cubes")) {
                    for (final JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                        cubes.add(parseCube(cubeElement.getAsJsonObject(), pivot, textureWidth, textureHeight, defaultMirror, defaultInflate));
                    }

                }

                final Vector3f parentPivot = absolutePivots.getOrDefault(parent, new Vector3f());

                bones.add(new GeoModelDefinition.BoneDefinition(
                        name, parent,
                        pivot.x() - parentPivot.x(), pivot.y() - parentPivot.y(), pivot.z() - parentPivot.z(),
                        rotation.x(), rotation.y(), rotation.z(),
                        !(bone.has("neverRender") && bone.get("neverRender").getAsBoolean()),
                        List.copyOf(cubes)
                ));

                warnUnsupported(bone, "poly_mesh", "texture_mesh", "texture_meshes", "locators",
                        "binding", "bind_pose_rotation", "render_group_id");
            }

        }

        return new GeoModelDefinition(List.copyOf(bones));
    }

    private static String parentName(JsonObject bone) {
        return bone.has("parent") ? bone.get("parent").getAsString() : "";
    }

    private static void validateHierarchy(Map<String, JsonObject> bones) {
        for (final Map.Entry<String, JsonObject> entry : bones.entrySet()) {
            final String parent = parentName(entry.getValue());
            if (!parent.isEmpty() && !bones.containsKey(parent)) {
                throw new IllegalArgumentException("[KnightLib] Bone '" + entry.getKey() + "' references unknown parent '" + parent + "'");
            }

        }

        for (final Map.Entry<String, JsonObject> entry : bones.entrySet()) {
            final Set<String> path = new HashSet<>();
            String current = entry.getKey();
            while (!current.isEmpty()) {
                if (!path.add(current)) {
                    throw new IllegalArgumentException("[KnightLib] Bone hierarchy cycle contains '" + current + "'");
                }

                current = parentName(bones.get(current));
            }

        }

    }

    private static void warnUnsupported(JsonObject object, String... names) {
        for (final String name : names) {
            if (object.has(name) && WARNED.add(name)) {
                KnightLib.LOGGER.warn("[KnightLib] Geometry feature '{}' is metadata-only and is not rendered", name);
            }

        }

    }

    private static GeoCube parseCube(JsonObject cube, Vector3f bonePivot, int textureWidth, int textureHeight, boolean defaultMirror, float defaultInflate) {
        final Vector3f origin = parseVector(cube, "origin");
        final Vector3f size = parseVector(cube, "size");

        final float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : defaultInflate;
        if (!Float.isFinite(inflate)) {
            throw new IllegalArgumentException("[KnightLib] Cube inflate must be finite");
        }

        final boolean mirror = cube.has("mirror") ? cube.get("mirror").getAsBoolean() : defaultMirror;

        final float x0 = -(origin.x() + size.x()) - inflate;
        final float y0 = origin.y() - inflate;
        final float z0 = origin.z() - inflate;
        final float extentX = size.x() + inflate * 2f;
        final float extentY = size.y() + inflate * 2f;
        final float extentZ = size.z() + inflate * 2f;

        // Per-cube rotation around its own pivot
        final Matrix4f transform = new Matrix4f();
        if (cube.has("rotation")) {
            final Vector3f pivot = parseVector(cube, "pivot").mul(-1, 1, 1);
            final Vector3f rotation = parseVector(cube, "rotation").mul(-1, -1, 1);
            transform.translate(pivot.x(), pivot.y(), pivot.z());
            transform.rotateZYX(
                    (float) Math.toRadians(rotation.z()),
                    (float) Math.toRadians(rotation.y()),
                    (float) Math.toRadians(rotation.x())
            );

            transform.translate(-pivot.x(), -pivot.y(), -pivot.z());
        }

        final Vector3f v1 = corner(transform, x0, y0, z0);
        final Vector3f v2 = corner(transform, x0 + extentX, y0, z0);
        final Vector3f v3 = corner(transform, x0, y0 + extentY, z0);
        final Vector3f v4 = corner(transform, x0, y0, z0 + extentZ);
        final Vector3f v5 = corner(transform, x0 + extentX, y0 + extentY, z0);
        final Vector3f v6 = corner(transform, x0, y0 + extentY, z0 + extentZ);
        final Vector3f v7 = corner(transform, x0 + extentX, y0, z0 + extentZ);
        final Vector3f v8 = corner(transform, x0 + extentX, y0 + extentY, z0 + extentZ);

        final Matrix3f normalMatrix = transform.normal(new Matrix3f());
        final Vector3f[] normals = new Vector3f[6];
        for (int i = 0; i < 6; i++) {
            normals[i] = normalMatrix.transform(BASE_NORMALS[i], new Vector3f()).normalize();
        }

        final JsonElement uv = cube.get("uv");
        final List<GeoCube.Face> faces = new ArrayList<>(6);

        if (uv != null && uv.isJsonObject()) {
            final JsonObject uvFaces = uv.getAsJsonObject();
            addFaceUv(faces, uvFaces, "north", v1, v2, v5, v3, normals[0], size.x(), size.y(), textureWidth, textureHeight, mirror);
            addFaceUv(faces, uvFaces, "east", v2, v7, v8, v5, normals[1], size.z(), size.y(), textureWidth, textureHeight, mirror);
            addFaceUv(faces, uvFaces, "south", v7, v4, v6, v8, normals[2], size.x(), size.y(), textureWidth, textureHeight, mirror);
            addFaceUv(faces, uvFaces, "west", v4, v1, v3, v6, normals[3], size.z(), size.y(), textureWidth, textureHeight, mirror);
            addFaceUv(faces, uvFaces, "up", v3, v5, v8, v6, normals[4], size.x(), size.z(), textureWidth, textureHeight, mirror);
            addFaceUv(faces, uvFaces, "down", v4, v7, v2, v1, normals[5], size.x(), size.z(), textureWidth, textureHeight, mirror);
        }
        else {
            float u = 0f;
            float v = 0f;
            if (uv != null && uv.isJsonArray()) {
                final JsonArray uvArray = uv.getAsJsonArray();
                u = uvArray.get(0).getAsFloat();
                v = uvArray.get(1).getAsFloat();
            }
            if (!Float.isFinite(u) || !Float.isFinite(v)) {
                throw new IllegalArgumentException("[KnightLib] Box UV values must be finite");
            }

            final float sx = (float) Math.floor(size.x());
            final float sy = (float) Math.floor(size.y());
            final float sz = (float) Math.floor(size.z());

            faces.add(boxFace(v1, v2, v5, v3, normals[0], u + sz + sx, v + sz + sy, u + sz, v + sz + sy, u + sz, v + sz, u + sz + sx, v + sz, textureWidth, textureHeight, mirror));
            faces.add(boxFace(v2, v7, v8, v5, normals[1], u + sz, v + sz + sy, u, v + sz + sy, u, v + sz, u + sz, v + sz, textureWidth, textureHeight, mirror));
            faces.add(boxFace(v7, v4, v6, v8, normals[2], u + sz + sx + sz + sx, v + sz + sy, u + sz + sx + sz, v + sz + sy, u + sz + sx + sz, v + sz, u + sz + sx + sz + sx, v + sz, textureWidth, textureHeight, mirror));
            faces.add(boxFace(v4, v1, v3, v6, normals[3], u + sz + sx + sz, v + sz + sy, u + sz + sx, v + sz + sy, u + sz + sx, v + sz, u + sz + sx + sz, v + sz, textureWidth, textureHeight, mirror));
            faces.add(boxFace(v3, v5, v8, v6, normals[4], u + sz + sx, v + sz, u + sz, v + sz, u + sz, v, u + sz + sx, v, textureWidth, textureHeight, mirror));
            faces.add(boxFace(v4, v7, v2, v1, normals[5], u + sz + sx + sx, v, u + sz + sx, v, u + sz + sx, v + sz, u + sz + sx + sx, v + sz, textureWidth, textureHeight, mirror));
        }

        // Cube vertices become relative to the owning bone pivot and are pre-scaled to blocks
        for (final GeoCube.Face face : faces) {
            for (final GeoCube.Vertex vertex : face.vertices()) {
                vertex.position().sub(bonePivot).mul(1f / 16f);
            }

        }

        return new GeoCube(faces.toArray(GeoCube.Face[]::new), size.x(), size.y(), size.z());
    }

    private static Vector3f corner(Matrix4f transform, float x, float y, float z) {
        return transform.transformPosition(new Vector3f(x, y, z));
    }

    private static void addFaceUv(List<GeoCube.Face> faces, JsonObject uvFaces, String key, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f normal, float defaultWidth, float defaultHeight, int textureWidth, int textureHeight, boolean mirror) {
        if (!uvFaces.has(key)) {
            return;
        }

        final JsonObject face = uvFaces.getAsJsonObject(key);
        final JsonArray uvArray = face.getAsJsonArray("uv");
        final float u = uvArray.get(0).getAsFloat();
        final float v = uvArray.get(1).getAsFloat();

        float width = defaultWidth;
        float height = defaultHeight;
        if (face.has("uv_size")) {
            JsonArray sizeArray = face.getAsJsonArray("uv_size");
            width = sizeArray.get(0).getAsFloat();
            height = sizeArray.get(1).getAsFloat();
        }
        if (!Float.isFinite(u) || !Float.isFinite(v) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("[KnightLib] Face UV values must be finite");
        }

        final int rotation = face.has("uv_rotation") ? face.get("uv_rotation").getAsInt() : 0;
        faces.add(
                boxFace(a, b, c, d, normal, u + width, v + height, u, v + height, u, v,
                u + width, v, textureWidth, textureHeight, mirror, rotation)
        );

    }

    private static GeoCube.Face boxFace(Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f normal, float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd, int textureWidth, int textureHeight, boolean mirror) {
        return boxFace(a, b, c, d, normal, ua, va, ub, vb, uc, vc, ud, vd, textureWidth, textureHeight, mirror, 0);
    }

    private static GeoCube.Face boxFace(Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f normal, float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd, int textureWidth, int textureHeight, boolean mirror, int rotation) {
        if (mirror) {
            float swap = ua;
            ua = ub;
            ub = swap;
            swap = uc;
            uc = ud;
            ud = swap;
        }

        final float[][] uv = {{ud, vd}, {uc, vc}, {ub, vb}, {ua, va}};
        final int turns = Math.floorMod(Math.round(rotation / 90f), 4);
        final Vector3f[] positions = {d, c, b, a};
        final GeoCube.Vertex[] vertices = new GeoCube.Vertex[4];
        for (int i = 0; i < 4; i++) {
            final float[] rotated = uv[Math.floorMod(i - turns, 4)];
            vertices[i] = new GeoCube.Vertex(new Vector3f(positions[i]), rotated[0] / textureWidth, rotated[1] / textureHeight);
        }

        return new GeoCube.Face(new Vector3f(normal), vertices);
    }

    private static Vector3f parseVector(JsonObject object, String key) {
        if (!object.has(key)) {
            return new Vector3f();
        }

        final JsonArray array = object.getAsJsonArray(key);
        final Vector3f value = new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
        if (!Float.isFinite(value.x()) || !Float.isFinite(value.y()) || !Float.isFinite(value.z())) {
            throw new IllegalArgumentException("[KnightLib] Geometry vector '" + key + "' must be finite");
        }

        return value;
    }

    public static Map<String, KnightLibAnimation> parseAnimations(JsonObject root) {
        return GeoAnimationParser.parse(root);
    }

}