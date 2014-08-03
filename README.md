Sondasplorer
============

Sondasplorer is a tool to analyse the performance of Java EE applications in distributed environments. 

Sondasplorer is highly extensible and customizable letting us to use probes already created and distributed along with the tool, as well as probes created specifically by the end user to analyze a specify part of the application. A very friendly API is provided to develop customized probes withing the Sondasplorer framework. Due to this framework we can isolate from the complexity of modifying Java bytecode in Java classes when you need to inspect what is causing a performance issue in an application

Between the probes provided in the standard distribution are:

Method probe: It let us to define in a configuration file which methods of an applitacion to inspect.

Servlet probe: It let us to indentify the actions maded from clients and so to assess the quality of service from the user point of view.

Sql probe: It let us to analyze the Sql sentences that are being executed as Statements and PreparedStatements. This probe moreover generates a list with the most expensive queries showing the parameters used when calling them in case they where PreparedStatements.

These three probes types generate statistical information about the number of invocations, total time, exclusive time, maximum time, minimum time and the error count.

In Sondasplorer framework a probe is a Java class designed to assess and/or modify any parameter in runtime. For example we can:

Assess methods total execution time

Assess methods exclusive execution time

Assess execution errors

Investigate methods calling parameters

Assess methods invocation count

Generate a performance execution tree

Detect memory leaks

Alter existing classes funcionality in a transparent way

Do whatever your imagination can create 

Sondasplorer can instrumentalize selected classes running in a Application Server (Websphere, Tomcat, etc.). This way, all the information collected by the probes will be sent very efficiently to the Collector Manager where existing endpoints will process it, send it to a client, or store it. Using the provided API, customized endpoints can be implemented to satisfy specific necessities of the user. For example, an endpoint can store the information in a Database where a client will access to process and show it. Another endpoint can store it in a file where a client can read it offline. Another endpoint can send the information by network to another machine where it will show the data in realtime. We can even have these EndPoints working simultaneously.

Sondasplorer has two working modes: full mode and the delta mode. In full mode, all information collected by the probes is sent to the Collector Manager without digesting. This is very useful to analyze the complete trace of a single execution in a load test with multiple threads executing concurrently but it has a high impact in the overall performance. Anyways, it is possible to enable and disable probes on demand. That allow us, for example, to enable the full-trace of a probe only when is needed and during the neccesary time. This is very useful in Preproduction environments where it is not possible to enable the full-trace mode from the beginning of a heavy test.

On the other hand, Sondasplorer has a delta mode where the information is condensed in quantums of data sent periodically. In this mode, very complex algorithms have been implented to reach the maximum performance without loosing any information. 


Full description
================

http://sites.google.com/site/sondasplorer
