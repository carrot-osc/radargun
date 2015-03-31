package org.radargun.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.radargun.logging.Log;
import org.radargun.logging.LogFactory;
import org.radargun.traits.Clustered;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class InfinispanServerClustered implements Clustered {
   protected final Log log = LogFactory.getLog(getClass());
   protected final InfinispanServerService service;
   protected final List<Membership> membershipHistory = new ArrayList<>();
   protected String lastMembers = null;

   public InfinispanServerClustered(InfinispanServerService service) {
      this.service = service;
      service.schedule(new Runnable() {
         @Override
         public void run() {
            checkAndUpdateMembership();
         }
      }, service.viewCheckPeriod);
   }

   @Override
   public boolean isCoordinator() {
      if (!service.lifecycle.isRunning()) {
         return false;
      }
      try {
         MBeanServerConnection connection = service.connection;
         if (connection == null) return false;
         ObjectName cacheManagerName = new ObjectName(getCacheManagerObjectName(service.jmxDomain, service.cacheManagerName));
         String nodeAddress = (String) connection.getAttribute(cacheManagerName, getNodeAddressAttribute());
         String clusterMembers = (String) connection.getAttribute(cacheManagerName, getClusterMembersAttribute());
         return clusterMembers.startsWith("[" + nodeAddress);
      } catch (Exception e) {
         log.error("Failed to retrieve data from JMX", e);
      }
      return false;
   }

   protected Collection<Member> checkAndUpdateMembership() {
      if (!service.lifecycle.isRunning()) {
         synchronized (this) {
            if (!membershipHistory.isEmpty() && membershipHistory.get(membershipHistory.size() - 1).members.size() > 0) {
               lastMembers = null;
               membershipHistory.add(Membership.empty());
            }
         }
         log.trace("Service is not running, returning empty members list.");
         return Collections.EMPTY_LIST;
      }
      try {
         MBeanServerConnection connection = service.connection;
         if (connection == null) {
            log.trace("MBeanServerConnection not available, returning empty members list.");
            return Collections.EMPTY_LIST;
         }
         ObjectName cacheManagerName = new ObjectName(getCacheManagerObjectName(service.jmxDomain, service.cacheManagerName));
         String nodeAddress = (String) connection.getAttribute(cacheManagerName, getNodeAddressAttribute());
         log.trace("Address returned by JMX query: " + nodeAddress);
         String membersString = (String) connection.getAttribute(cacheManagerName, getClusterMembersAttribute());
         log.trace("Members returned by JMX query: " + membersString);
         synchronized (this) {
            if (lastMembers != null && lastMembers.equals(membersString)) {
               Collection membershipHistoryMembers = membershipHistory.get(membershipHistory.size() - 1).members;
               log.trace(String.format("Last members %s not null, returning members %s from membership history.", lastMembers, membershipHistoryMembers));
               return membershipHistoryMembers;
            }
            if (!membersString.startsWith("[") || !membersString.endsWith("]")) {
               throw new IllegalArgumentException("Unexpected members string format: " + membersString);
            }
            lastMembers = membersString;
            String[] nodes = membersString.substring(1, membersString.length() - 1).split(",", 0);
            ArrayList<Member> members = new ArrayList<>();
            for (int i = 0; i < nodes.length; ++i) {
               members.add(new Member(nodes[i].trim(), nodes[i].equals(nodeAddress), i == 0));
            }
            membershipHistory.add(Membership.create(members));
            log.trace("Returning members " + members);
            return members;
         }
      } catch (Exception e) {
         log.error("Failed to retrieve data from JMX", e);
         return null;
      }
   }

   @Override
   public Collection<Member> getMembers() {
      return checkAndUpdateMembership();
   }

   @Override
   public synchronized List<Membership> getMembershipHistory() {
      return new ArrayList<>(membershipHistory);
   }

   protected String getClusterMembersAttribute() {
      return "clusterMembers";
   }

   protected String getNodeAddressAttribute() {
      return "nodeAddress";
   }

   protected String getClusterSizeAttribute() {
      return "clusterSize";
   }

   private String getCacheManagerObjectName(String jmxDomain, String managerName) {
      return String.format("%s:type=CacheManager,name=\"%s\",component=CacheManager", jmxDomain, managerName);
   }
}
