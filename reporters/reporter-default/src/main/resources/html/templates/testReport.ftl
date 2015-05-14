<html>
<head>
   <title>TEST</title>
   <script></script>
</head>
<body>
   <h1>Test ${testReport.testName}</h1>
   <#list (testReport.testAggregations.results())?keys as aggregationKey>
      <h2${aggregationKey}></h2>
      <table>
         <#if (testReport.maxIterations > 1)>
            <tr>
               <th colspan="2"/>
               <#if ((testReport.testAggregations.results())[aggregationKey]?size > 0)>
                  <#assign testResultEntry = (testReport.testAggregations.results())[0]/>
               </#if>
               <#list 0..testReport.maxIterations as iteration>
                  <#if testResultEntry??>
                     <#assign testResult = testResultEntry.value.get(iteration)/>;
                     <#assign iterationName = testResult.iteration.test.iterationsName/>;
                     <#if testResult?? && iterationName??>
                        <#assign iterationValue = (iterationName + "=" + testResult.iteration.value)/>
                     <#else>
                        <#assign iterationValue = ("iteration " + iteration)/>
                     </#if>
                  <#else>
                     <#assign iterationValue = ("iteration " + iteration)/>
                  </#if>
                  <th style="border-left-color: black; border-left-width: 2px;">${iterationValue}</th>
               </#list>
            </tr>
         </#if>
         <tr>
            <th colspan="2">Configuration</th>
            <#list 0..testReport.maxIterations as iteration>
               <th style="text-align: center; border-left-color: black; border-left-width: 2px">Value</th>
            </#list>
         </tr>
         <#list ((testReport.testAggregations.results())[aggregationKey])?keys as testResultKey>
            <#if ((testReport.testAggregations.results())[aggregationKey])[testResultKey]?size == 0>
               <#assign nodeCount = 0 />
            <#else>
               <#assign nodeCount = ((testReport.testAggregations.results())[aggregationKey])[testResultKey].get(0).slaveResults.size />
            </#if>
            <#list (((testReport.testAggregations.results())[aggregationKey])[testResultKey])?keys as aggregationTestResult>
               <td style="border-left-color: black; border-left-width: 2px;">${((testReport.testAggregations.results())[aggregationKey])[testResultKey]}</td>
            </#list>
         </#list>
      </table>
      <li>
         <a href="test_${testName}.html">${testName}</a>
      </li>
   </#list>
</body>
</html>
