package org.radargun.stages.cache.background;

import java.util.List;

import org.radargun.DistStageAck;
import org.radargun.config.Property;
import org.radargun.config.Stage;
import org.radargun.stages.AbstractDistStage;
import org.radargun.stages.cache.test.LoadDataStage;
import org.radargun.utils.TimeConverter;

/**
 * @author Matej Cimbora &lt;mcimbora@redhat.com&gt;
 */
@Stage(doc = "Stops data loading process started by BackgroundLoadDataStartStage.")
public class BackgroundLoadDataStopStage extends AbstractDistStage {

   private static final String BACKGROUND_LOADERS = "BackgroundLoaders";

   @Property(converter = TimeConverter.class, doc = "Maximum time to wait for loading threads to finish. By default, " +
         "wait until the threads finish their job.")
   private long timeoutDuration = 0;

   @Override
   public DistStageAck executeOnSlave() {
      List<LoadDataStage.Loader> loaders = (List<LoadDataStage.Loader>) slaveState.get(BACKGROUND_LOADERS);
      if (loaders != null) {
         for (LoadDataStage.Loader loader : loaders) {
            try {
               if (timeoutDuration > 0) {
                  loader.join(timeoutDuration);
               } else {
                  loader.join();
               }
            } catch (InterruptedException e) {
               return errorResponse("Exception while interrupting loading thread.", e);
            }
         }
      }
      return successfulResponse();
   }
}
