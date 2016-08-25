Getting Some Structure
======================

Okay, so we've made got Concourse setup and we've made a pipeline and we've even externalised some of our configurations.  Now its time to get serious about our pipeline.  Time to make something that could be used in production.   

The Steps in our Build
---
When building our application for deployment to a production server there are a number of steps the build has to go through.  Each step performs a specific function.  The steps we need to consider are:

* Compile the code.
* Run all the unit tests.
* Build an arefact we can deploy to a test environment.



(Somewhere, we do these things)

* Increment version numbers. (if we have something that can be released).
* Run all the integration tests. (which may require other systems to be setup).
* Package the application
* Push the resulting artefact to a repository.

 **how does the settings.xml get set? **

###Setting up our initial properties and resources

1. In our last tutorial we externalised some of our commonly used properties.  We'll use that approach again.  Ensure that we have a `properties` folder that contains a `simplejava-github-properties.yml` file.  That file should contain details of our github repo and the basic settings we want to use for our maven build tool:
	
	```
	github-uri: https://github.com/neildunlop/simplejava.git
	github-branch: master
	maven-opts: # -Xms256m -Xmx512m
	maven-config: # -s path/to/settings.xml
	```
	
2. In our `pipeline.yml` file we need a resource that points to the github repository holding our code.  So, like we have done before, ensure `pipeline.yml` contains:

	```
	---
#define the resources that are available as inputs to jobs.
resources:
#GitHub repository that holds all the code that makes up our application
  - name: git-repo
    type: git
    source:
      uri: {{github-uri}}
      branch: {{github-branch}}

	```

###Unit Test Job

The first and most obvious job of any build pipeline is to compile the code.  This job will also run all of our unit tests.  We're going to restructure our pipeline a little here as its going to get significantly bigger in this tutorial.  The first step is to put all our tasks in a `tasks` sub-folder just to keep things tidy.

1.  Navigate to the `build` folder we created earlier and create a new folder in there called `tasks`.
 
    ```
    mkdir tasks
    ```

2.  Navigate into the tasks folder.

    ```
    cd tasks
    ```
    
3.  Make a new file to contain our unit test job:

    ```
    touch task-unit-test.yml
    ```
    
4.  Edit the newly created `task-unit-test.yml` so that contains the following content:

    ```
    ---
    platform: linux
    
    #define the base docker image used to build the container that will host 
    #this task while it executes.
    #Alpine Linux with Java 8 and Maven 3.3
    image_resource:
      type: docker-image
      source:
        repository: andreptb/maven
        tag: 'latest'
    
    params:
      MAVEN_OPTS:
      MAVEN_CONFIG:
    
    inputs:
      - name: git-repo
    
    #execute mvn package against the target pom
    run:
      path: simplejava/build/tasks/unit-test.sh
      args: [--input-dir, simplejava]
    ```

    As we have done previously, this job sets up an environment to host the task while it runs.  As before we have used a docker image of a small linux based environment that contains Java and Maven.  The only slight change we have made from our previous job definition is that we have moved the definition of the actual command to be executed to another file.  This simply helps us to seperate the setup of the task environment from the execution of the task.   
    
    TODO: Discuss the 'task-unit-test.sh' - it contains a lot of boilerplate and uses the maven wrapper - safer in the long run.. but verbose.
    
    **TODO: Discuss the MAVEN\_OPTS and MAVEN\_CONFIG entry**

###Unit Test Script
As mentioned above, we have separated the setup for our `unit-test` job from the execution of that job.  The execution simply executes commands on the command line but this can get reasonably complicated and we want to wrap some error handling around that.  All this sits inside a script file for each job.  By convention, the name of the script matches the name of the job.

1. Ensure you are inside the `tasks` folder and create a new file:

	```
	touch task-unit-test.sh
	```
	
2. Edit the file to contain the following:

	```
	#!/bin/sh

	inputDir=
	
	while [ $# -gt 0 ]; do
	  case $1 in
	    -i | --input-dir )
	      inputDir=$2
	      shift
	      ;;
	    * )
	      echo "Unrecognized option: $1" 1>&2
	      exit 1
	      ;;
	  esac
	  shift
	done
	
	error_and_exit() {
	  echo $1 >&2
	  exit 1
	}
	
	if [ ! -d "$inputDir" ]; then
	  error_and_exit "missing input directory: $inputDir"
	fi
	
	cd $inputDir
	
	./mvnw clean test
	```
	Our shell script simply expects to run maven in a target directory. (The last line is where the action happens).  Despite the length of the script, all its actually doing is checking to make sure that an input directory was specified when the script was invoked, and if an input directory wasn't supplied then you get a reasonable error message.
	
###Adding a Job to the Pipeline
With all our supporting files created we can now add a job to our pipeline to actually make it do something.
	  
1.  Open the `pipeline.yml` file and add a new `job`:

    ```
	#The things that we want our concourse server to do.
	jobs:
	  - name: unit-test
	    public: true
	    plan:
	      - get: git-repo
	        trigger: true
	      - task: unit
	        file: simplejava/build/tasks/task-unit-test.yml
	        params: &MAVENPARAMS
	          MAVEN_OPTS: {{maven-opts}}
	          MAVEN_CONFIG: {{maven-config}}
    ```
Note the YAML `anchor` (&MAVENPARAMS) that allows us to name and then reuse a block of YAML content.  In this case we have named our Maven parameters so that is easier to reuse those settings elsewhere in our pipeline.  See [the official YAML tutorial](https://learnxinyminutes.com/docs/yaml/) for more details.


###Executing our Build Pipeline
Now that we have defined our build pipeline we need to submit it to Concourse before we can do anything with it.  

1.  Ensure you are in the '**build**' directory we created earlier.

2.  Commit it to the concourse server with:

	```
	fly -t myconcourse set-pipeline -p simple-java-build-pipeline -c pipeline.yml -l ./properties/simplejava-github-properties.yml
	```
	**-t** specifies the target Concourse.  We use the convienient alias we setup earlier.
	
	**-p** specifies the name we want to give our newly created pipeline.
	
	**-c** specifies the content file that we want to upload.
	
	**-l** specifies the properties file we want to use to provide values for all the properties placeholders we have used.

3.  You'll be asked if you want to apply the configuration.  Enter **'Y'**.

4.  Open your web browser and navigate to [the Concourse web UI](http://192.168.100.4:8080/).  You should see your new pipeline listed.


Supporting resources:

https://www.youtube.com/watch?v=YntGTVjAOsY - video of setting up a Java build pipline (details about maven settings etc)
http://pcf-hugo-workshop.cfapps.io/2016/03/15/lab-6-build-pipelines-using-concourse.ci/ (overview of putting a pipeline together... scroll down to section 2 for a good example pipeline)
https://github.com/rjain-pivotal/PCF-demo - An actual fully featured pipeline