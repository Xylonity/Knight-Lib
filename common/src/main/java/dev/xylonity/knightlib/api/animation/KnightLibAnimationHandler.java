package dev.xylonity.knightlib.api.animation;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.item.KnightLibAnimatedItem;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import dev.xylonity.knightlib.network.packets.AnimationSyncS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Per-instance animation state for a {@link KnightLibAnimatable} entity/block entity or a particular {@link ItemStack}
 */
public final class KnightLibAnimationHandler {

    public static final String MAIN_CONTROLLER = "main";

    private static final int MAX_CONTROLLERS = 64;
    private static final int MAX_TRANSITION_TICKS = 1200;
    private static final int MAX_DURATION_TICKS = 20 * 60 * 60 * 24;

    private final Map<String, Controller> controllers = new LinkedHashMap<>();

    private final AnimationTarget target;

    private List<ClientControllerBinding> clientControllerBindings = List.of();
    private KnightLibAnimationState clientState;
    private boolean clientControllersRegistered;

    private WeakReference<Level> lastTickLevel = new WeakReference<>(null);
    private long lastTickGameTime = Long.MIN_VALUE;

    private KnightLibAnimationHandler(AnimationTarget target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    public static <T extends Entity & KnightLibAnimatable> KnightLibAnimationHandler of(T entity) {
        return new KnightLibAnimationHandler(new EntityTarget<>(Objects.requireNonNull(entity, "entity")));
    }

    public static <T extends BlockEntity & KnightLibAnimatable> KnightLibAnimationHandler of(T blockEntity) {
        return new KnightLibAnimationHandler(new BlockEntityTarget<>(Objects.requireNonNull(blockEntity, "blockEntity")));
    }

    static KnightLibAnimationHandler of(KnightLibAnimatedItem item, ItemStack stack, Level level) {
        return new KnightLibAnimationHandler(new ItemStackTarget(Objects.requireNonNull(item, "item"), Objects.requireNonNull(stack, "stack"), level));
    }

    void updateItemContext(ItemStack stack, Level level, Entity holder) {
        target.updateItemContext(stack, level, holder);
    }

    public Controller controller(String name) {
        validateControllerName(name);
        final Controller existing = controllers.get(name);
        if (existing != null) {
            return existing;
        }

        if (controllers.size() >= MAX_CONTROLLERS) {
            final Iterator<Map.Entry<String, Controller>> iterator = controllers.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().animation() == null) {
                    iterator.remove();
                    break;
                }

            }

        }

        if (controllers.size() >= MAX_CONTROLLERS) {
            throw new IllegalStateException("[KnightLib] An animatable cannot have more than " + MAX_CONTROLLERS + " active controllers");
        }

        final Controller created = new Controller(name);
        controllers.put(name, created);

        return created;
    }

    public Collection<Controller> controllers() {
        return Collections.unmodifiableCollection(controllers.values());
    }

    Controller findController(String name) {
        return controllers.get(name);
    }

    /**
     * Entity this handler animates, or null for a block-entity/item handler
     */
    public Entity entity() {
        return target.entity();
    }

    /**
     * Block entity this handler animates, or null for an entity/item handler
     */
    public BlockEntity blockEntity() {
        return target.blockEntity();
    }

    public KnightLibAnimatable owner() {
        return target.owner();
    }

    public void play(KnightLibAnim animation) {
        Objects.requireNonNull(animation, "animation");
        playAt(animation.controller(), animation.steps(), animation.transitionTicks(),
                animation.transitionEasing(), animation.speed(), gameTime(),
                animation.durationTicks(), animation.blendMode()
        );

    }

    public void play(String controllerName, String animation, int transitionTicks, KnightLibEasings transitionEasing, float speed) {
        play(controllerName, animation, transitionTicks, transitionEasing, speed, 0);
    }

    public void play(String controllerName, String animation, int transitionTicks, KnightLibEasings transitionEasing, float speed, int durationTicks) {
        playAt(controllerName, animation, transitionTicks, transitionEasing, speed, gameTime(), durationTicks);
    }

    void playAt(String controllerName, String animation, int transitionTicks, KnightLibEasings transitionEasing, float speed, long startedAt, int durationTicks) {
        playAt(controllerName, List.of(new KnightLibAnim.Step(animation, KnightLibAnim.PlaybackMode.ONCE)),
                transitionTicks, transitionEasing, speed, startedAt, durationTicks,
                KnightLibAnimationBlendMode.AUTHORED
        );

    }

    private void playAt(String controllerName, List<KnightLibAnim.Step> steps, int transitionTicks, KnightLibEasings transitionEasing, float speed, long startedAt, int durationTicks, KnightLibAnimationBlendMode blendMode) {
        validateControllerName(controllerName);
        if (steps == null || steps.isEmpty() || !KnightLibAnim.isValidSequence(steps)) {
            throw new IllegalArgumentException("[KnightLib] animation sequence must contain 1.." + KnightLibAnim.MAX_STEPS + " valid steps");
        }

        validateTransition(transitionTicks);
        if (!Float.isFinite(speed) || speed <= 0f || speed > 100f) {
            throw new IllegalArgumentException("[KnightLib] speed must be finite and in (0, 100]");
        }
        if (durationTicks < 0 || durationTicks > MAX_DURATION_TICKS) {
            throw new IllegalArgumentException("[KnightLib] durationTicks must be in [0, " + MAX_DURATION_TICKS + "]");
        }

        Objects.requireNonNull(blendMode, "blendMode");
        final Controller controller = controller(controllerName);
        controller.command(steps, transitionTicks, transitionEasing, speed, startedAt, durationTicks, blendMode);

        sync(controller);
    }

    public void stop(String controllerName, int transitionTicks) {
        validateControllerName(controllerName);
        validateTransition(transitionTicks);
        final Controller controller = controllers.get(controllerName);
        if (controller == null || controller.animation() == null) {
            return;
        }

        controller.command(List.of(), transitionTicks,
                KnightLibAnimatable.DEFAULT_TRANSITION_EASING, 1f, gameTime(), 0,
                controller.blendMode()
        );

        sync(controller);
    }

    public void tick() {
        final Level level = level();
        if (level == null) {
            return;
        }

        final long now = level.getGameTime();
        if (lastTickLevel.get() == level && lastTickGameTime == now) {
            return;
        }

        lastTickLevel = new WeakReference<>(level);
        lastTickGameTime = now;

        if (level.isClientSide()) {
            updateClientControllers();
            return;
        }

        boolean removedStoppedItemController = false;
        final Iterator<Controller> iterator = controllers.values().iterator();
        while (iterator.hasNext()) {
            final Controller controller = iterator.next();
            if (controller.animation() != null && controller.durationTicks() > 0 && (now - controller.commandGameTime()) * controller.speed() >= controller.durationTicks()) {
                controller.command(List.of(), controller.transitionTicks(), controller.easing(), 1f, now, 0, controller.blendMode());
                sync(controller);
            }
            else if (target.shouldDiscardStoppedController(controller, now)) {
                iterator.remove();
                removedStoppedItemController = true;
            }

        }

        target.afterServerTick(this, removedStoppedItemController);
    }

    private void updateClientControllers() {
        if (!clientControllersRegistered) {
            final List<ClientControllerBinding> bindings = new ArrayList<>();
            target.registerAnimationControllers(bindings);
            clientControllerBindings = List.copyOf(bindings);
            clientControllersRegistered = true;
        }

        if (clientControllerBindings.isEmpty()) {
            return;
        }

        // One instance per handler
        if (clientState == null) {
            clientState = new KnightLibAnimationState(this);
        }

        clientState.refresh(target.animationEntity(), target.blockEntity(), clientItemStack(), level());

        for (final ClientControllerBinding binding : clientControllerBindings) {
            binding.update(this);
        }

    }

    private ItemStack clientItemStack() {
        return target instanceof ItemStackTarget ? target.itemStack() : null;
    }

    /**
     * Applies what a controller returned
     */
    private void applySelection(String name, int stopTransitionTicks, KnightLibAnim selected) {
        final Controller current = controllers.get(name);
        if (selected == null) {
            if (current != null && current.animation() != null) {
                stop(name, stopTransitionTicks);
            }

            return;
        }

        final KnightLibAnim animation = selected.controller(name);
        if (!matches(current, animation)) {
            play(animation);
            return;
        }

        // Speed is deliberately absent from matches, as it would be reset
        current.retime(animation.speed());
    }

    private boolean matches(Controller controller, KnightLibAnim animation) {
        return controller != null
                && controller.steps().equals(animation.steps())
                && controller.transitionTicks() == animation.transitionTicks()
                && controller.easing() == animation.transitionEasing()
                && controller.blendMode() == animation.blendMode()
                && controller.durationTicks() == animation.durationTicks();
    }

    /**
     * Writes controller state without any client-only data
     */
    public void save(CompoundTag ownerTag) {
        final ListTag list = new ListTag();
        for (final Controller controller : controllers.values()) {
            final CompoundTag tag = new CompoundTag();
            tag.putString("Controller", controller.name());
            tag.putString("Animation", controller.animation() == null ? "" : controller.animation());

            final ListTag steps = new ListTag();
            for (final KnightLibAnim.Step step : controller.steps()) {
                final CompoundTag stepTag = new CompoundTag();
                stepTag.putString("Animation", step.animation());
                stepTag.putInt("Mode", step.mode().id());
                steps.add(stepTag);
            }

            tag.put("Steps", steps);
            tag.putLong("StartedAt", controller.commandGameTime());
            tag.putInt("Transition", controller.transitionTicks());
            tag.putInt("Easing", controller.easing().id());
            tag.putFloat("Speed", controller.speed());
            tag.putInt("Duration", controller.durationTicks());
            tag.putInt("BlendMode", controller.blendMode().id());

            list.add(tag);
        }


        ownerTag.put(KnightLibItemAnimations.TAG, list);
    }

    /**
     * Restores controller state from entity/block-entity/item data or an update snapshot
     */
    public void load(CompoundTag ownerTag) {
        final ListTag list = ownerTag.contains(KnightLibItemAnimations.TAG, Tag.TAG_LIST) ? ownerTag.getList(KnightLibItemAnimations.TAG, Tag.TAG_COMPOUND) : new ListTag();
        final Map<String, SavedController> restored = new LinkedHashMap<>();
        final int count = Math.min(list.size(), 64);
        for (int i = 0; i < count; i++) {
            final CompoundTag tag = list.getCompound(i);
            final String name = tag.getString("Controller");
            final String savedAnimation = tag.getString("Animation");
            final float speed = tag.getFloat("Speed");
            if (name.isBlank() || name.length() > 64 || !Float.isFinite(speed) || speed <= 0f || speed > 100f || restored.containsKey(name)) {
                continue;
            }

            final List<KnightLibAnim.Step> steps = readSteps(tag, savedAnimation);
            if (steps == null) {
                continue;
            }

            final int transition = Math.min(Math.max(tag.getInt("Transition"), 0), MAX_TRANSITION_TICKS);
            final KnightLibEasings easing = KnightLibEasings.byId(tag.getInt("Easing"));
            final long startedAt = tag.getLong("StartedAt");
            final int duration = Math.min(Math.max(tag.getInt("Duration"), 0), MAX_DURATION_TICKS);
            final KnightLibAnimationBlendMode blendMode;
            try {
                blendMode = KnightLibAnimationBlendMode.byId(tag.getInt("BlendMode"));
            }
            catch (Exception exception) {
                continue;
            }

            restored.put(name, new SavedController(steps, transition, easing, speed, startedAt, duration, blendMode));
        }

        for (final Controller controller : controllers.values()) {
            if (controller.animation() != null && !restored.containsKey(controller.name())) {
                controller.command(List.of(), KnightLibAnimatable.DEFAULT_TRANSITION_TICKS, KnightLibAnimatable.DEFAULT_TRANSITION_EASING, 1f, gameTime(), 0, controller.blendMode());
            }

        }

        for (final Map.Entry<String, SavedController> entry : restored.entrySet()) {
            final SavedController saved = entry.getValue();
            final Controller current = controller(entry.getKey());
            if (!current.matches(saved.steps(), saved.transitionTicks(), saved.easing(), saved.speed(), saved.startedAt(), saved.durationTicks(), saved.blendMode())) {
                current.command(saved.steps(), saved.transitionTicks(), saved.easing(), saved.speed(), saved.startedAt(), saved.durationTicks(), saved.blendMode());
            }

        }

    }

    private static List<KnightLibAnim.Step> readSteps(CompoundTag tag, String legacyAnimation) {
        if (!tag.contains("Steps", Tag.TAG_LIST)) {
            if (legacyAnimation.isEmpty()) {
                return List.of();
            }

            try {
                return List.of(new KnightLibAnim.Step(legacyAnimation, KnightLibAnim.PlaybackMode.ONCE));
            }
            catch (Exception exception) {
                return null;
            }

        }

        final ListTag savedSteps = tag.getList("Steps", Tag.TAG_COMPOUND);
        if (savedSteps.size() > KnightLibAnim.MAX_STEPS) {
            return null;
        }

        final List<KnightLibAnim.Step> steps = new ArrayList<>(savedSteps.size());
        try {
            for (int i = 0; i < savedSteps.size(); i++) {
                final CompoundTag stepTag = savedSteps.getCompound(i);
                steps.add(new KnightLibAnim.Step(
                        stepTag.getString("Animation"),
                        KnightLibAnim.PlaybackMode.byId(stepTag.getInt("Mode"))));
            }

        }
        catch (Exception exception) {
            return null;
        }

        return KnightLibAnim.isValidSequence(steps) ? List.copyOf(steps) : null;
    }

    public String getActiveAnimation(String controllerName) {
        final Controller controller = controllers.get(controllerName);
        return controller == null ? null : controller.animation();
    }

    public boolean isAnimationActive(String animation) {
        if (animation == null) {
            return false;
        }

        for (final Controller controller : controllers.values()) {
            for (final KnightLibAnim.Step step : controller.steps()) {
                if (animation.equals(step.animation())) {
                    return true;
                }

            }

        }

        return false;
    }

    public Entity animationEntity() {
        return target.animationEntity();
    }

    public void dispatchKeyframe(KnightLibKeyframeEvent event) {
        target.dispatchKeyframe(event);
    }

    public void dispatchFinished(String controller, String animation) {
        target.dispatchFinished(controller, animation);
    }

    /**
     * Applies an animation received from the server. Internal, called by KnightLib's packet handler.
     */
    public void applyRemote(String controllerName, List<KnightLibAnim.Step> steps, int transitionTicks, int easingId, float speed, long commandGameTime) {
        applyRemote(controllerName, steps, transitionTicks, easingId, speed, commandGameTime, KnightLibAnimationBlendMode.AUTHORED);
    }

    public void applyRemote(String controllerName, List<KnightLibAnim.Step> steps, int transitionTicks, int easingId, float speed, long commandGameTime, KnightLibAnimationBlendMode blendMode) {
        validateControllerName(controllerName);
        validateTransition(transitionTicks);
        if (steps == null || !KnightLibAnim.isValidSequence(steps) || !Float.isFinite(speed) || speed <= 0f || speed > 100f || commandGameTime < 0L || blendMode == null) {
            throw new IllegalArgumentException("[KnightLib] Invalid remote animation command");
        }

        final KnightLibEasings easing = KnightLibEasings.byId(easingId);
        controller(controllerName).command(steps, transitionTicks, easing, speed, commandGameTime, 0, blendMode);
    }

    private Level level() {
        return target.level();
    }

    private long gameTime() {
        final Level level = level();
        return level == null ? 0L : level.getGameTime();
    }

    private static void validateControllerName(String name) {
        if (name == null || name.isBlank() || name.length() > 64) {
            throw new IllegalArgumentException("[KnightLib] controllerName must contain 1..64 characters");
        }

    }

    private static void validateTransition(int ticks) {
        if (ticks < 0 || ticks > MAX_TRANSITION_TICKS) {
            throw new IllegalArgumentException("[KnightLib] transitionTicks must be in [0, " + MAX_TRANSITION_TICKS + "]");
        }

    }

    private void sync(Controller controller) {
        target.sync(this, controller);
    }

    private sealed interface AnimationTarget permits EntityTarget, BlockEntityTarget, ItemStackTarget {

        Level level();

        default Entity entity() {
            return null;
        }

        default BlockEntity blockEntity() {
            return null;
        }

        default KnightLibAnimatable owner() {
            throw new IllegalStateException("[KnightLib] Item-stack handlers do not have a KnightLibAnimatable owner");
        }

        default Entity animationEntity() {
            return null;
        }

        default ItemStack itemStack() {
            throw new IllegalStateException("[KnightLib] Only an item handler has an ItemStack");
        }

        default void updateItemContext(ItemStack stack, Level level, Entity holder) {
            throw new IllegalStateException("Only an item handler has item context");
        }

        default void registerAnimationControllers(List<ClientControllerBinding> bindings) {
            owner().registerAnimationControllers(new ClientRegistrar(bindings));
        }

        default void dispatchKeyframe(KnightLibKeyframeEvent event) {
            owner().onAnimationKeyframe(event);
        }

        default void dispatchFinished(String controller, String animation) {
            owner().onAnimationFinished(controller, animation);
        }

        default boolean shouldDiscardStoppedController(Controller controller, long now) {
            return false;
        }

        default void afterServerTick(KnightLibAnimationHandler handler, boolean discardedController) {
            ;;
        }

        void sync(KnightLibAnimationHandler handler, Controller controller);

        default void syncAllTo(KnightLibAnimationHandler handler, ServerPlayer player) {
            final Level level = level();
            if (level == null || level.isClientSide()) {
                return;
            }

            for (final Controller controller : handler.controllers.values()) {
                if (controller.animation() == null) {
                    continue;
                }

                KnightLib.NET.sendTo(player, AnimationSyncS2C.TYPE.base(), handler.buildSyncMessage(controller));
            }

        }

    }

    private record EntityTarget<T extends Entity & KnightLibAnimatable>(
            T entity
    ) implements AnimationTarget {

        private EntityTarget {
            Objects.requireNonNull(entity, "entity");
        }

        @Override
        public Level level() {
            return entity.level();
        }

        @Override
        public KnightLibAnimatable owner() {
            return entity;
        }

        @Override
        public Entity animationEntity() {
            return entity;
        }

        @Override
        public void sync(KnightLibAnimationHandler handler, Controller controller) {
            final Level level = level();
            if (level == null || level.isClientSide()) {
                return;
            }

            final AnimationSyncS2C message = handler.buildSyncMessage(controller);
            KnightLib.NET.sendToTracking(entity, AnimationSyncS2C.TYPE.base(), message);

            if (entity instanceof ServerPlayer player) {
                KnightLib.NET.sendTo(player, AnimationSyncS2C.TYPE.base(), message);
            }

        }

    }

    private record BlockEntityTarget<T extends BlockEntity & KnightLibAnimatable>(
            T blockEntity
    ) implements AnimationTarget {

        private BlockEntityTarget {
            Objects.requireNonNull(blockEntity, "blockEntity");
        }

        @Override
        public Level level() {
            return blockEntity.getLevel();
        }

        @Override
        public KnightLibAnimatable owner() {
            return blockEntity;
        }

        @Override
        public void sync(KnightLibAnimationHandler handler, Controller controller) {
            final Level level = level();
            if (level == null || level.isClientSide()) {
                return;
            }

            blockEntity.setChanged();
            KnightLib.NET.sendToTracking(level, blockEntity.getBlockPos(), AnimationSyncS2C.TYPE.base(), handler.buildSyncMessage(controller));
        }

    }

    private static final class ItemStackTarget implements AnimationTarget {

        private final KnightLibAnimatedItem item;
        private WeakReference<ItemStack> stack;
        private WeakReference<Level> level;
        private WeakReference<Entity> holder = new WeakReference<>(null);

        private ItemStackTarget(KnightLibAnimatedItem item, ItemStack stack, Level level) {
            this.item = Objects.requireNonNull(item, "item");
            this.stack = new WeakReference<>(validateStack(stack));
            this.level = new WeakReference<>(level);
        }

        @Override
        public Level level() {
            return level.get();
        }

        @Override
        public Entity animationEntity() {
            return holder.get();
        }

        @Override
        public ItemStack itemStack() {
            return stack.get();
        }

        @Override
        public void updateItemContext(ItemStack stack, Level level, Entity holder) {
            this.stack = new WeakReference<>(validateStack(stack));
            if (level != null) {
                this.level = new WeakReference<>(level);
            }
            if (holder != null) {
                this.holder = new WeakReference<>(holder);
            }

        }

        @Override
        public void registerAnimationControllers(List<ClientControllerBinding> bindings) {
            item.registerAnimationControllers(new ItemClientRegistrar(bindings));
        }

        @Override
        public void dispatchKeyframe(KnightLibKeyframeEvent event) {
            final ItemStack stack = itemStack();
            if (stack != null) {
                item.onAnimationKeyframe(stack, event);
            }

        }

        @Override
        public void dispatchFinished(String controller, String animation) {
            final ItemStack stack = itemStack();
            if (stack != null) {
                item.onAnimationFinished(stack, controller, animation);
            }

        }

        @Override
        public boolean shouldDiscardStoppedController(Controller controller, long now) {
            return controller.animation() == null && now - controller.commandGameTime() >= controller.transitionTicks();
        }

        @Override
        public void afterServerTick(KnightLibAnimationHandler handler, boolean discardedController) {
            if (discardedController || handler.controllers.isEmpty()) {
                persist(handler);
            }

        }

        @Override
        public void sync(KnightLibAnimationHandler handler, Controller controller) {
            final Level level = level();
            if (level == null || !level.isClientSide()) {
                persist(handler);
            }

        }

        @Override
        public void syncAllTo(KnightLibAnimationHandler handler, ServerPlayer player) {
            ;;
        }

        private ItemStack validateStack(ItemStack stack) {
            Objects.requireNonNull(stack, "stack");
            if (item instanceof Item vanillaItem && stack.getItem() != vanillaItem) {
                throw new IllegalArgumentException("[KnightLib] The ItemStack no longer belongs to this handler");
            }

            return stack;
        }

        private void persist(KnightLibAnimationHandler handler) {
            final ItemStack stack = itemStack();
            if (stack == null) {
                return;
            }

            if (!handler.controllers.isEmpty()) {
                handler.save(stack.getOrCreateTag());
                return;
            }

            final CompoundTag ownerTag = stack.getTag();
            if (ownerTag == null) {
                return;
            }

            ownerTag.remove(KnightLibItemAnimations.TAG);
            if (ownerTag.isEmpty()) {
                stack.setTag(null);
            }

        }

    }

    private record SavedController(
            List<KnightLibAnim.Step> steps,
            int transitionTicks,
            KnightLibEasings easing,
            float speed,
            long startedAt,
            int durationTicks,
            KnightLibAnimationBlendMode blendMode
    ) {
        ;;
    }

    private interface ClientControllerBinding {

        void update(KnightLibAnimationHandler handler);

    }

    private record SelectedControllerBinding(
            String name,
            int stopTransitionTicks,
            Function<KnightLibAnimationState, KnightLibAnim> selector
    ) implements ClientControllerBinding {

        @Override
        public void update(KnightLibAnimationHandler handler) {
            handler.clientState.controller(name);
            handler.applySelection(name, stopTransitionTicks, selector.apply(handler.clientState));
        }

    }

    private static final class TriggerControllerBinding implements ClientControllerBinding {

        private final String name;
        private final Predicate<KnightLibAnimationState> trigger;
        private final KnightLibAnim animation;
        private boolean previous;

        private TriggerControllerBinding(String name, Predicate<KnightLibAnimationState> trigger, KnightLibAnim animation) {
            this.name = name;
            this.trigger = trigger;
            this.animation = animation.controller(name);
        }

        @Override
        public void update(KnightLibAnimationHandler handler) {
            handler.clientState.controller(name);
            final boolean active = trigger.test(handler.clientState);
            if (active && !previous) {
                handler.play(animation);
            }

            previous = active;
        }

    }

    private record SelectedItemControllerBinding(
            String name,
            int stopTransitionTicks,
            Function<KnightLibAnimationState, KnightLibAnim> selector
    ) implements ClientControllerBinding {

        @Override
        public void update(KnightLibAnimationHandler handler) {
            if (handler.clientState.stack() == null) {
                return;
            }

            handler.clientState.controller(name);
            handler.applySelection(name, stopTransitionTicks, selector.apply(handler.clientState));
        }

    }

    private static final class TriggerItemControllerBinding implements ClientControllerBinding {

        private final String name;
        private final Predicate<KnightLibAnimationState> trigger;
        private final KnightLibAnim animation;
        private boolean previous;

        private TriggerItemControllerBinding(String name, Predicate<KnightLibAnimationState> trigger, KnightLibAnim animation) {
            this.name = name;
            this.trigger = trigger;
            this.animation = animation.controller(name);
        }

        @Override
        public void update(KnightLibAnimationHandler handler) {
            if (handler.clientState.stack() == null) {
                return;
            }

            handler.clientState.controller(name);
            final boolean active = trigger.test(handler.clientState);
            if (active && !previous) {
                handler.play(animation);
            }

            previous = active;
        }

    }

    private static final class ClientRegistrar implements KnightLibAnimationControllerRegistrar {

        private final List<ClientControllerBinding> bindings;
        private final Set<String> names = new HashSet<>();

        private ClientRegistrar(List<ClientControllerBinding> bindings) {
            this.bindings = bindings;
        }

        @Override
        public void add(String controllerName, int stopTransitionTicks, Supplier<KnightLibAnim> selector) {
            Objects.requireNonNull(selector, "selector");
            add(controllerName, stopTransitionTicks, state -> selector.get());
        }

        @Override
        public void add(String controllerName, int stopTransitionTicks, Function<KnightLibAnimationState, KnightLibAnim> selector) {
            claim(controllerName);
            validateTransition(stopTransitionTicks);
            bindings.add(new SelectedControllerBinding(controllerName, stopTransitionTicks, Objects.requireNonNull(selector, "selector")));
        }

        @Override
        public void addTrigger(String controllerName, BooleanSupplier trigger, KnightLibAnim animation) {
            Objects.requireNonNull(trigger, "trigger");
            addTrigger(controllerName, state -> trigger.getAsBoolean(), animation);
        }

        @Override
        public void addTrigger(String controllerName, Predicate<KnightLibAnimationState> trigger, KnightLibAnim animation) {
            claim(controllerName);
            bindings.add(new TriggerControllerBinding(controllerName, Objects.requireNonNull(trigger, "trigger"), Objects.requireNonNull(animation, "animation")));
        }

        private void claim(String controllerName) {
            validateControllerName(controllerName);
            if (!names.add(controllerName)) {
                throw new IllegalArgumentException("[KnightLib] Controller registered twice: " + controllerName);
            }
            if (names.size() > MAX_CONTROLLERS) {
                throw new IllegalStateException("[KnightLib] An animatable cannot register more than " + MAX_CONTROLLERS + " controllers");
            }

        }

    }

    private static final class ItemClientRegistrar implements KnightLibItemAnimationControllerRegistrar {

        private final List<ClientControllerBinding> bindings;
        private final Set<String> names = new HashSet<>();

        private ItemClientRegistrar(List<ClientControllerBinding> bindings) {
            this.bindings = bindings;
        }

        @Override
        public void add(String controllerName, int stopTransitionTicks, Function<KnightLibAnimationState, KnightLibAnim> selector) {
            claim(controllerName);
            validateTransition(stopTransitionTicks);
            bindings.add(new SelectedItemControllerBinding(controllerName, stopTransitionTicks, Objects.requireNonNull(selector, "selector")));
        }

        @Override
        public void addTrigger(String controllerName, Predicate<KnightLibAnimationState> trigger, KnightLibAnim animation) {
            claim(controllerName);
            bindings.add(new TriggerItemControllerBinding(controllerName, Objects.requireNonNull(trigger, "trigger"), Objects.requireNonNull(animation, "animation")));
        }

        private void claim(String controllerName) {
            validateControllerName(controllerName);
            if (!names.add(controllerName)) {
                throw new IllegalArgumentException("[KnightLib] Controller registered twice: " + controllerName);
            }
            if (names.size() > MAX_CONTROLLERS) {
                throw new IllegalStateException("[KnightLib] An item stack cannot register more than " + MAX_CONTROLLERS + " controllers");
            }

        }

    }

    public void syncAllTo(ServerPlayer player) {
        target.syncAllTo(this, player);
    }

    private AnimationSyncS2C buildSyncMessage(Controller controller) {
        final Entity entity = target.entity();
        final BlockEntity blockEntity = target.blockEntity();
        if (entity == null && blockEntity == null) {
            throw new IllegalStateException("[KnightLib] Item-stack animation targets cannot build tracking packets");
        }

        return new AnimationSyncS2C(
                entity != null,
                entity != null ? entity.getId() : 0,
                blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO,
                controller.name(),
                controller.steps(),
                controller.transitionTicks(),
                controller.easing().id(),
                controller.speed(),
                controller.commandGameTime(),
                controller.blendMode()
        );

    }

    public static final class Controller {

        private final String name;

        private List<KnightLibAnim.Step> steps = List.of();
        private long commandGameTime;
        private int transitionTicks = KnightLibAnimatable.DEFAULT_TRANSITION_TICKS;
        private KnightLibEasings easing = KnightLibAnimatable.DEFAULT_TRANSITION_EASING;
        private float speed = 1f;
        private int durationTicks;
        private KnightLibAnimationBlendMode blendMode = KnightLibAnimationBlendMode.AUTHORED;
        private long sequence;

        /**
         * Client render clock, managed by KnightLib's animator. Do not touch.
         */
        public Object clientState;

        private Controller(String name) {
            this.name = name;
        }

        void command(List<KnightLibAnim.Step> steps, int transitionTicks, KnightLibEasings easing, float speed, long gameTime, int durationTicks, KnightLibAnimationBlendMode blendMode) {
            this.steps = List.copyOf(steps);
            this.transitionTicks = Math.max(0, transitionTicks);
            this.easing = easing == null ? KnightLibAnimatable.DEFAULT_TRANSITION_EASING : easing;
            this.speed = speed <= 0f ? 1f : speed;
            this.commandGameTime = gameTime;
            this.durationTicks = Math.max(0, durationTicks);
            this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
            this.sequence++;
        }

        void retime(float speed) {
            this.speed = !Float.isFinite(speed) || speed <= 0f ? 1f : speed;
        }

        boolean matches(List<KnightLibAnim.Step> steps, int transitionTicks, KnightLibEasings easing, float speed, long gameTime, int durationTicks, KnightLibAnimationBlendMode blendMode) {
            return this.steps.equals(steps)
                    && this.transitionTicks == transitionTicks
                    && this.easing == easing
                    && Float.compare(this.speed, speed) == 0
                    && this.commandGameTime == gameTime
                    && this.durationTicks == durationTicks
                    && this.blendMode == blendMode;
        }

        public String name() {
            return name;
        }

        public String animation() {
            return steps.isEmpty() ? null : steps.get(0).animation();
        }

        public List<KnightLibAnim.Step> steps() {
            return steps;
        }

        public long commandGameTime() {
            return commandGameTime;
        }

        public int transitionTicks() {
            return transitionTicks;
        }

        public KnightLibEasings easing() {
            return easing;
        }

        public float speed() {
            return speed;
        }

        public long sequence() {
            return sequence;
        }

        public int durationTicks() {
            return durationTicks;
        }

        public KnightLibAnimationBlendMode blendMode() {
            return blendMode;
        }

    }

}