---
#
# Use the widgets beneath and the content will be
# inserted automagically in the webpage. To make
# this work, you have to use › layout: frontpage
#
layout: frontpage
title: "DKPro Lab"
---

DKPro Lab is a lightweight framework for parameter sweeping experiments. It allows to set up experiments consisting of multiple interdependent tasks in a declarative manner with minimal overhead. Parameters are injected into tasks using via annotated class fields. Data produced by a task for any particular parameter configuration is stored and re-used whenever possible to avoid the needless recalculation of results. Reports can be attached to each task to post-process the experimental results and present them in a convenient manner, e.g. as tables or charts.

The source code is provided under the Apache Software License (ASL) version 2. 

The source code including examples is already available from the Git repository here. A binary release via Maven will be prepared soon. If you are interested in the project, please subscribe to the [users group](http://groups.google.com/group/dkpro-lab-user).

## Using DKPro Lab

  * [Mallet in UIMA: experiments with ClearTK and DKPro Lab][WERC-2011] (Slides, PDF, 10 Jun 2011, Lab version 0.6.0)

## Who uses DKPro Lab?

  * [Ubiquitous Knowledge Processing Lab](http://www.ukp.tu-darmstadt.de )
  * [DKPro Text Classification](https://dkpro.github.io/dkpro-tc)
  * [Search on Ohloh Code Search...](http://code.ohloh.net/search?s=%22de.tudarmstadt.ukp.dkpro.lab%22&browser=Default&mp=1&ml=1&me=1&md=1&filterChecked=true)

## How to cite

If you use DKPro Lab in scientific work, please cite

> Eckart de Castilho, R. and Gurevych, I. (2011). **A lightweight framework for reproducible parameter sweeping in information retrieval**. In Proceedings of the 2011 workshop on Data infrastructurEs for supporting information retrieval evaluation, DESIRE ’11, pages 7–10, New York, NY, USA. ACM. [(pdf)][DESIRE-2011] [(bib)][DESIRE-2011-BIB]

[DESIRE-2011]: http://www.ukp.tu-darmstadt.de/fileadmin/user_upload/Group_UKP/publikationen/2011/CIKM-DESIRE-2011-rec-ig-submitted.pdf
[DESIRE-2011-BIB]: http://www.ukp.tu-darmstadt.de/publications/details/?no_cache=1&tx_bibtex_pi1%5Bpub_id%5D=TUD-CS-2011-0215&type=99&tx_bibtex_pi1%5Bbibtex%5D=yes
[WERC-2011]: http://www.werc.tu-darmstadt.de/fileadmin/user_upload/GROUP_WERC/LKE/tutorials/ML-tutorial-5a.pdf 