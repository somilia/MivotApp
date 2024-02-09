Objectif du projet
==================

The goal of this project is to generate MIVOT annotations on VOTables.


Connection with VOLLT
=====================
This part describes the connection between our extension and VOLLT framework.

Generate a new output format
----------------------------
MangoFormat implements the OutputFormat interface and 
allows us to propose our own output format.
The class MangoFormat has been added in the `output_format` property of `tap.properties` file.

The `CustomVOTableFormat` and `MangoFormat` classes, 
as well as the annotation cleanup functions, 
have been retrieved from the xtapdb project (Ihsane).

Getting the connection of TAP
-----------------------------

A new class `TAPMivotFactory` have been created, 
it overrides the initial `TAPFactory` class of 
VOLLT to retrieve the instance of TAP.

The TAPMivotFactory class has been added in the
`tap_factory` property of the `tap.properties` file.

Getting the RealPath of the web-app folder
------------------------------------------
The realpath can be got by using the `ServletContext` object.
I extended the `ConfigurableTAPServlet` in `MivotTAPServlet` and overrode the init method to get the `ServletContext` and store it in a static variable. 
Then I added my `MivotTAPServlet` to the web.xml file.
In this way, I was able to access to the `ServletContext` from the `MivotTAPServlet` class.

```java
MivotTAPServlet.servletContext.getRealPath("/");
```


Building the mapping dictionary
===============================
The mapping dictionary is built as ModelBase objects.
It is the MivotBaseInit class that is responsible for building the mapping dictionary.
For building it from the columns requested, 
we look for 3 information by querying our rule mapping table "mango_mivot":
- To which dmrole are these columns mapped?
- From which dmtype do these dmroles originate?
- What are the mandatory dmroles for these dmtypes?
- 
Once these three information are obtained through queries to the mapping table, 
the next step is to determine if all mandatory dmroles 
for each dmtype are present in the request.

Building the annotation
=======================


`already_built` is a dictionary used to know if the annotation has already been built for this dmtype.
`dmtypeErrorDone` is a dictionary used to know if the error class has already been built.

Improvement to do
=================

The following points should be improved:
- Revamp to structure of the mapping table
- Change the condition "CoordSys" with 
- The error class handling is not well tested and should be improved. 
  Can several table with the same dmerror been managed ?
- The resolve_ref method should be divided into several methods.
- Several exception class could be added regarding MIVOT. 
- Remove `ModelBase.link_id` because it is not used. Can be used for error class ?
- The VOLLT logger could be used instead of the System.out.println.
- We can add a `frameDone` dictionary to know if the frame has already been built.
- Add indentation to the annotation (with XMLUtils).
- Find a better name for `resolve_ref` method and rename `changeAttributeValue` in resolve name?.
- How to handle dmtype (snippet) importation if this one is already in the request?


