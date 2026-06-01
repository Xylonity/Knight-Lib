package dev.xylonity.knightlib.api.event;

/**
 * Base type for all KnightLib events
 */
public abstract class KnightLibEvent {

   /**
    * Whether this event is eligible for sticky replay
    */
   public boolean isSticky() {
      return false;
   }

}
