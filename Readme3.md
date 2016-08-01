Getting Some Structure
======================

Okay, so we've made got Concourse setup and we've made a pipeline and we've even externalised some of our configurations.  Now its time to get serious about our pipeline.  Time to make something that could be used in production.

The Steps in our Build
---
When building our application for deployment to a production server there are a number of steps the build has to go through.  Each step performs a specific function.  The steps we need to consider are:

* Build the code.
* Run all the unit tests.
* Build an arefact we can deploy to a test environment.



(Somewhere, we do these things)

* Increment version numbers. (if we have something that can be released).
* Run all the integration tests. (which may require other systems to be setup).


* Package the application
* Push the resulting artefact to a repository.



Unit Test Job
---
I
