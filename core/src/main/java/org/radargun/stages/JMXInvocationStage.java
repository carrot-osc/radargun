package org.radargun.stages;

import java.lang.management.ManagementFactory;
import java.util.Collection;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.radargun.DistStageAck;
import org.radargun.config.Property;
import org.radargun.config.Stage;

/**
 * @author Matej Cimbora &lt;mcimbora@redhat.com&gt;
 */
@Stage(doc = "Allows to invoke JMX-exposed methods.")
public class JMXInvocationStage extends AbstractDistStage {

   @Property(optional = false, doc = "Query to find particular MBeans.")
   private String query;

   @Property(optional = false, doc = "Name of the method to be invoked.")
   private String methodName;

   @Property(doc = "Method parameters.")
   private Object[] parameters;

   @Property(doc = "Method parameter signatures.")
   private String[] signatures;

   @Property(doc = "Continue method invocations if failure occurs. Default is false.")
   private boolean continueOnFailure = false;

   @Override
   public DistStageAck executeOnSlave() {
      try {
         MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
         Collection<ObjectInstance> queryResult = getQueryResult(mbeanServer);
         if (queryResult == null || queryResult.isEmpty()) {
            return errorResponse("Specified query '" + query + "' returned no results.");
         }
         for (ObjectInstance objectInstance : queryResult) {
            try {
               log.trace("Invoking method " + methodName);
               mbeanServer.invoke(objectInstance.getObjectName(), methodName, parameters, signatures);
            } catch (Exception e) {
               if (continueOnFailure) {
                  continue;
               } else {
                  return errorResponse("Exception while invoking method " + methodName + ".", e);
               }
            }
         }
      } catch (MalformedObjectNameException e) {
         return errorResponse("Exception while performing query '" + query + "'.", e);
      }
      return new DistStageAck(slaveState);
   }

   private Collection<ObjectInstance> getQueryResult(MBeanServer mBeanServer) throws MalformedObjectNameException {
      return mBeanServer.queryMBeans(new ObjectName(query), null);
   }
}
