package io.hyperfoil.api.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Intended base for all builders that might need relocation when the step is copied over.
 */
public interface BuilderBase<S extends BuilderBase<S>> {
   default void prepareBuild() {}

   @SuppressWarnings("unchecked")
   default S setLocator(Locator locator) {
      return (S) this;
   }

   /**
    * Should be overridden if the {@link #setLocator(Locator)} is used.
    * If the locator is not used it is legal to return <code>this</code>.
    *
    * @param locator The place where the copy should be inserted.
    * @return Deep copy of this object.
    */
   @SuppressWarnings("unchecked")
   default S copy(Locator locator) {
      return (S) this;
   }

   static <T extends BuilderBase<T>> List<T> copy(Locator locator, Collection<T> builders) {
      return builders.stream().map(b -> b.copy(locator)).collect(Collectors.toList());
   }
}
