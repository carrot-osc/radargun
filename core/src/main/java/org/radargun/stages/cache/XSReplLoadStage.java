package org.radargun.stages.cache;

import org.radargun.DistStageAck;
import org.radargun.config.Property;
import org.radargun.config.Stage;
import org.radargun.stages.AbstractDistStage;
import org.radargun.stages.cache.generators.CacheAwareTextGenerator;
import org.radargun.stages.cache.generators.KeyGenerator;
import org.radargun.stages.cache.generators.StringKeyGenerator;
import org.radargun.stages.cache.generators.ValueGenerator;
import org.radargun.stages.helpers.Range;
import org.radargun.traits.BasicOperations;
import org.radargun.traits.CacheInformation;
import org.radargun.traits.InjectTrait;

/**
 * Loads data into the cache with input cache name encoded into the value.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@Stage(doc = "Loads data into the cache with input cache name encoded into the value.")
public class XSReplLoadStage extends AbstractDistStage {

   @Property(optional = false, doc = "Amount of entries that should be inserted into the cache.")
   private int numEntries;

   @Property(doc = "If set to true, the entries are removed instead of being inserted. Default is false.")
   private boolean delete = false;

   @Property(doc = "String encoded into the value so that the entry may be distinguished from entries loaded in " +
         "different load stages. Default is empty string.")
   private String valuePostFix = "";

   @InjectTrait(dependency = InjectTrait.Dependency.MANDATORY)
   private CacheInformation cacheInformation;

   @InjectTrait(dependency = InjectTrait.Dependency.MANDATORY)
   private BasicOperations basicOperations;


   public DistStageAck executeOnSlave() {
      KeyGenerator keyGenerator = (KeyGenerator) slaveState.get(KeyGenerator.KEY_GENERATOR);
      if (keyGenerator == null) {
         keyGenerator = new StringKeyGenerator();
      }
      ValueGenerator valueGenerator = (ValueGenerator) slaveState.get(ValueGenerator.VALUE_GENERATOR);
      if (valueGenerator == null) {
         valueGenerator = new CacheAwareTextGenerator(cacheInformation.getDefaultCacheName(), valuePostFix);
      }
      String cacheName = cacheInformation.getDefaultCacheName();
      BasicOperations.Cache cache = basicOperations.getCache(cacheName);
      Range myRange = Range.divideRange(numEntries, slaveState.getGroupSize(), slaveState.getIndexInGroup());
      for (int i = myRange.getStart(); i < myRange.getEnd(); ++i) {
         try {
            Object key = keyGenerator.generateKey(i);
            if (!delete) {
               cache.put(key, valueGenerator.generateValue(key, -1, null));
            } else {
               cache.remove(key);
            }
         } catch (Exception e) {
            log.error("Error inserting key " + i + " into " + cacheName);
         }
      }
      return successfulResponse();
   }
}
