# DKPro Lab

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/dkpro/dkpro-lab?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.tudarmstadt.ukp.dkpro.lab/dkpro-lab-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.tudarmstadt.ukp.dkpro.lab/dkpro-lab-core)

DKPro Lab is a lightweight framework for parameter sweeping experiments. It allows to set up 
experiments consisting of multiple interdependent tasks in a declarative manner with minimal 
overhead. Parameters are injected into tasks using via annotated class fields. Data produced by a 
task for any particular parameter configuration is stored and re-used whenever possible to avoid 
the needless recalculation of results. Reports can be attached to each task to post-process the 
experimental results and present them in a convenient manner, e.g. as tables or charts.
