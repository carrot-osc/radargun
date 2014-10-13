package org.radargun.stages;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.radargun.DistStageAck;
import org.radargun.config.Property;
import org.radargun.config.Stage;
import org.radargun.traits.InjectTrait;
import org.radargun.traits.JmxConnectionProvider;
import org.radargun.utils.PrimitiveValue;

/**
 * @author Matej Cimbora &lt;mcimbora@redhat.com&gt;
 */
@Stage(doc = "Allows to invoke JMX-exposed methods.")
public class JMXInvocationStage extends AbstractDistStage {

   @Property(optional = false, doc = "Method will be invoked on all ObjectInstances matching given query.")
   private String query;

   @Property(optional = false, doc = "Name of the method to be invoked.")
   private String methodName;

   @Property(doc = "Method parameters.", complexConverter = PrimitiveValue.ListConverter.class)
   private List<PrimitiveValue> methodParameters;

   @Property(doc = "Method parameter signatures.")
   private String[] methodSignatures;

   @Property(doc = "Continue method invocations if an exception occurs. Default is false.")
   private boolean continueOnFailure = false;

   @Property(doc = "Expected result value. If specified, results of method invocations are compared with this value.",
         complexConverter = PrimitiveValue.ObjectConverter.class)
   private PrimitiveValue expectedResult;

   @InjectTrait
   private JmxConnectionProvider jmxConnectionProvider;

   @Override
   public DistStageAck executeOnSlave() {
      if ((methodParameters != null || !methodParameters.isEmpty()) && (methodSignatures == null || methodParameters.size() != methodSignatures.length)) {
         return errorResponse("Signatures need to be specified for individual method parameters.");
      }
      MBeanServerConnection connection = null;
      if (jmxConnectionProvider != null) {
         JMXConnector connector = jmxConnectionProvider.getConnector();
         if (connector != null) {
            try {
               connection = connector.getMBeanServerConnection();
            } catch (IOException e) {
               return errorResponse("Failed to connect to MBean server.", e);
            }
         } else {
            return errorResponse("Failed to connect to MBean server.");
         }
      } else {
         connection = ManagementFactory.getPlatformMBeanServer();
      }
      Collection<ObjectInstance> queryResult = null;
      try {
         queryResult = getQueryResult(connection);
         if (queryResult == null || queryResult.isEmpty()) {
            return errorResponse(String.format("Specified query '%s' returned no results.", query));
         }
      } catch (Exception e) {
         return errorResponse(String.format("Exception while performing query '%s'", query), e);
      }
      List<Object> values = new ArrayList<>();
      if (methodParameters != null) {
         for (PrimitiveValue primitiveValue : methodParameters) {
            values.add(primitiveValue.getElementValue());
         }
      }
      for (ObjectInstance objectInstance : queryResult) {
         try {
            log.trace("Invoking method " + methodName);
            Object result = connection.invoke(objectInstance.getObjectName(), methodName, values.toArray(new Object[methodParameters.size()]), methodSignatures);
            if (expectedResult != null && !expectedResult.getElementValue().equals(result)) {
               return errorResponse(String.format("Method invocation returned incorrect result. Expected '%s', was '%s'.", expectedResult.getElementValue(), result));
            }
         } catch (Exception e) {
            if (continueOnFailure) {
               continue;
            } else {
               return errorResponse(String.format("Exception while invoking method '%s'.", methodName), e);
            }
         }
      }
      return new DistStageAck(slaveState);
   }

   private Collection<ObjectInstance> getQueryResult(MBeanServerConnection connection) throws MalformedObjectNameException, IOException {
      return connection.queryMBeans(new ObjectName(query), null);
   }
}
