<html>
   <head>
      <title>TEST</title>
      <script></script>
   </head>
   <#macro writeProperty name definition>
      <#if definition.getClass().getSimpleName() == "SimpleDefinition">
      <li>
         ${name} : ${definition}
      </li>
      <#elseif definition.getClass().getSimpleName() == "ComplexDefinition">
      <li>
         <ul>
            <#list definition.attributes?keys as attributeKey>
               <@writeProperty name=attributeKey definition=(definition.attributes)[attributeKey]/>
            </#list>
         </ul>
      </li>
      </#if>
   </#macro>
   <body>
      <h1>RadarGun benchmark report</h1>
      <h2>Tests</h2>
      <ul>
         <#list reporter.allTests as testName>
            <li>
               <a href="test_${testName}.html">${testName}</a>
            </li>
         </#list>
      </ul>
      <h2>Configurations</h2>
      The benchmark was executed on following configurations and cluster sizes: <br/>
      <ul>
         <#list reporter.reports as report>
            <li>
               <b>${report.configuration.name}</b> on cluster with ${report.cluster.size} slaves: ${report.cluster}. <br/>
               Setups: <br/>
               <ul>
                  <#list report.configuration.setups as setup>
                     <li>
                        Group: ${setup.group}
                        <ul>
                           <li>Plugin: ${setup.plugin}</li>
                           <li>Service: ${setup.service}</li>
                           <#if (indexDocument.getConfigs(report))?size == 0>
                              <li>Configuration file: ${setup.properties["file"]}</li>
                           <#elseif (indexDocument.getConfigs(report))?size == 1>
                              <!-- -->
                           <#else>
                              <li>
                                 Configuration files:
                                 <ul>
                                    <#list (indexDocument.getConfigs(report)) as config>
                                       <#if config.filename == setup.properties["file"]>
                                          <li>
                                             <a href="original_${setup.configuration.name}_${setup.group}_${report.cluster.clusterIndex}_${config.filename}">${config.filename}</a>
                                             <#if (config.slaves?size == report.cluster.getSlaves(setup.group)?size)>
                                                (slaves
                                                 <#list config.slaves as slaveIndex>
                                                   ${slaveIndex}<#if slaveIndex_has_next>,</#if>
                                                 </#list>
                                                )
                                             </#if>
                                          </li>
                                       </#if>
                                    </#list>
                                    <#list (indexDocument.getConfigs(report)) as config>
                                       <#if config.filename != setup.properties["file"]>
                                          <li>
                                             <a href="original_${setup.configuration.name}_${setup.group}_${report.cluster.clusterIndex}_${config.filename}">${config.filename}</a>
                                             <#if (config.slaves?size == report.cluster.getSlaves(setup.group)?size)>
                                                (slaves
                                                <#list config.slaves as slaveIndex>
                                                   ${slaveIndex}<#if slaveIndex_has_next>,</#if>
                                                </#list>
                                                )
                                             </#if>
                                          </li>
                                       </#if>
                                    </#list>
                                 </ul>
                              </li>
                           </#if>
                        </ul>
                        <#if setup.properties??>
                           <ul>
                              <#list setup.properties?keys as property>
                                 <@writeProperty name=property definition=(setup.properties)[property] />
                              </#list>
                           </ul>
                        </#if>
                     </li>
                  </#list>
               </ul>
            </li>
         </#list>
      </ul>
      <h2>Scenario</h2>
      Note that some properties may have not been resolved correctly as these depend on local properties. <br/>
      These stages have been used for the benchmark: <br/>
      <ul>
         <#if reporter.reports?? && reporter.reports?has_content>
            <#list reporter.reports[0].stages as stage>
               <li>
                  <span style="cursor:pointer;" onclick="">
                     ${stage.name}
                  </span>
                  <ul>
                     <#list stage.properties as property>
                        <#if property.definition??>
                           <li>
                              <b>${property.name} = ${property.value}</b>
                           </li>
                        <#else>
                           <li style="display: none">
                              ${property.name} = ${property.value}
                           </li>
                        </#if>
                     </#list>
                  </ul>
               </li>
            </#list>
         </#if>
      </ul>
      <h2>Timelines</h2>
      <ul>
         <#list reporter.reports as report>
            <li>
               <a href="timeline_${report.configuration.name}_${report.cluster.clusterIndex}.html">${report.configuration.name} on ${report.cluster}</a>
            </li>
         </#list>
      </ul>
      <hr/>
      Generated on ${.now} by RadarGun
      JDK: ${System.getProperty("java.vm.name")} (${System.getProperty("java.vm.version")}, ${System.getProperty("java.vm.vendor")})
      OS: ${System.getProperty("os.name")} (${System.getProperty("os.version")}, ${System.getProperty("os.arch")})
   </body>
</html>
