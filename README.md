Sondasplorer
============

Sondasplorer is a tool to analyse the performance of Java EE applications in distributed environments. 

Sondasplorer is highly extensible and customizable letting us to use probes already created and distributed along with the tool, as well as probes created specifically by the end user to analyze a specify part of the application. A very friendly API is provided to develop customized probes withing the Sondasplorer framework. Due to this framework we can isolate from the complexity of modifying Java bytecode in Java classes when you need to inspect what is causing a performance issue in an application

Between the probes provided in the standard distribution are:

Method probe: It let us to define in a configuration file which methods of an applitacion to inspect.

Servlet probe: It let us to indentify the actions maded from clients and so to assess the quality of service from the user point of view.

Sql probe: It let us to analyze the Sql sentences that are being executed as Statements and PreparedStatements. This probe moreover generates a list with the most expensive queries showing the parameters used when calling them in case they where PreparedStatements.

These three probes types generate statistical information about the number of invocations, total time, exclusive time, maximum time, minimum time and the error count.

Full description
================

http://sites.google.com/site/sondasplorer
