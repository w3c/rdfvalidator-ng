RDFValidator-NG
===============

Check and Visualize your RDF documents.

RDFValidator-NG is basically the code at [http://dev.w3.org/cvsweb/java/classes/org/w3c/rdf/examples/ARPServlet.java](http://dev.w3.org/cvsweb/java/classes/org/w3c/rdf/examples/ARPServlet.java) (which currently runs [http://www.w3.org/RDF/Validator/](http://www.w3.org/RDF/Validator/). This version makes very easy to run the service locally.

How to start geeking
--------------------

You only a recent version of Java. If you want to generate images for the graphs you provide, please install GraphViz.

```bash
git clone git://github.com/w3c/rdfvalidator-ng.git
cd rdfvalidator-ng/
./sbt assembly
java -jar target/rdf-validator.jar 8080
```

Then you can go to [http://localhost:8080](http://localhost:8080).

Note: we made a bundle available at https://www.w3.org/2013/02/rdf-validator.jar .

Licence
-------

This source code is made available under the [W3C Licence](http://opensource.org/licenses/W3C).
