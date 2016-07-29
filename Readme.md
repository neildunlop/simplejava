Building a Simple Java Application with Concourse CI
====================================================

Once you understand a couple of key concepts and know a few key conventions its pretty easy to get up and running with Concourse CI.  So, we'll walk through an example of using Concourse CI to build a simple Java application.  The application itself does almost nothing, its just there so we have some code to build.

Our first convention is that the files that define your Concourse build pipeline are stored alongside the code that is being built, so this usually means we have a 'build' folder at the top level of our project.  For your typical java application this means we have a 'build' folder alongside our 'src' directory.

Our second convention is that our Concourse server is usually a seperate machine that is used to execute the build pipeline.  For the sake of getting up and running quickly we'll use Vagrant to host our Concourse CI server as if it was a remote machine.

Once we have our build pipeline defined, we'll upload it to the Concourse server and then we can use the Concourse UI to trigger and monitor builds.

Getting our Concourse Server Setup
----------------------------------
The Concourse CI server is normally deployed on a remote server that is dedicated to running your builds.  Because we are working locally, we'll put the Concourse Server inside a Vagrant machine.  This has the added benefit of making it quite easy to get Concourse up and running with the minimum config.


1. [Download Vagrant](https://www.vagrantup.com/downloads.html) and follow the installation instructions.

2.  In the top level directory of our Java project, at the same level as the '**src**' folder, create a new directory to hold the build pipeline and associated scripts and navigate into it:

	```
	mkdir build
	cd build
	```
	 
3. Create a `Vagrantfile` that will allow us to start up our Concourse CI server inside a Vagrant machine:
    (this handily creates a Vagrantfile for us with all the right configuration to spin up an instance of Concourse)
    
   ```
    	vagrant init concourse/lite     
   ```

4. Start up our vagrant machine

   ```
    	vagrant up --provider virtualbox
   ```

5.  Navigate to `http://192.168.100.4:8080/` to access the Concourse UI running inside our vagrant machine.  
    ![Concourse Inital UI](/images/Screen Shot 2016-07-29 at 17.16.18.png)


6.  Download the '**Fly CLI**' application for your operating system.  The Fly CLI allows us to manage remote concourse servers from the command line of our local machine.
	If you are on Linux or OSX you'll have make it executable and add it to your path using:

	```
    	sudo mkdir -p /usr/local/bin
		sudo mv ~/Downloads/fly /usr/local/bin
		sudo chmod 0755 /usr/local/bin/fly
	```

7.  When submitting pipelines to the Concourse server from the command line we have to specify the address of the target Concourse server, creating an alisas makes this *much* easier. Alias your concourse server so its easier to use it from the command line with:


	```
	fly --target myconcourse login  --concourse-url http://192.168.100.4:8080
	fly -t myconcourse sync
	```


Thats it...Concourse setup and ready to do some work.


Putting Concourse to Work
---------------------
###Concourse Essentials
Now that our Concourse server is up and running, we want to do something useful with it.  There are three main concepts in the Concourse world.

 * **Resources** are things that you can do something to.  The most obvious resource for us is the github repository that contains the code we want to build.
  
 * **Jobs** can be thought of as functions with inputs and outputs that automatically run when inputs are updated.  A job can depend on the output of an upstream job, which is the foundation of stringing jobs together in a pipeline.  Jobs can contain a number of tasks that specify a number of things that need to happen to get the job done.
 
 * **Tasks** are the execution of a script in an isolated environment which has all the resources available that that the task needs to function.  If the script exits with the result `0` then the task succeeds, if not, the task fails.  Tasks can be executed by a Job or executed manually for the Fly CLI.  Tasks are the main building blocks of Concourse.  They perform the real work.

To be honest, Resources are faily intuitive but the line between Jobs and Tasks is still a little blurred in my head.  The best approach is to try and build a pipleine and see how Jobs and Tasks differ when you try and do something useful.

###Defining Our Build Pipeline
We'll keep our pipeline really simple for now.  All we want our pipeline to do is check out the latest version of our code from the master branch of our github repository every 60 seconds and build the code, running all tests.
	
1.  Ensure you are in the '**build**' directory we created earlier.

2.  Create a file to hold the definition of our build pipline which details everything we want to do in our build and all the resouces we'll use during that build.

	```
		touch pipeline.yml
	```

3.  Edit the '**pipeline.yml**' we just created with your favourite text editor.

	```
		nano pipeline.yml
	```
	
4.  First we'll add a resource that tells Concourse where our source code is held so that it can check it out whenever its needed.  Add the following content to '**pipeline.yml**':

	```
		---
       #define the resources that are available as inputs to jobs.
       resources:
       #GitHub repository that holds all the code that makes up our application
  		 - name: git-repo
    	   type: git
    		source:
      		  uri: https://github.com/neildunlop/simplejava.git
            branch: master
	```
In the snippet above we've added the top level **resources** element which holds definitions of all the useful resources that our pipeline needs.  We've added one resource for now which is all the details needed to access our github repository.  The name can be whatever you like, it will appear in the Concourse UI so try and make it meaningful.  The source its https in this case but it could be ssh if you prefer.  Its just the URL you use to clone the repository.

5.  Add another resource that gives us a '**time**' resource that has the expressed purpose of trigger our build pipeline at the time interval we specify.  In our case, we'll do this every 60 seconds.  Add the following to '**pipeline.yml** just after the resoure we added in step 4':

	```
		#A simple timer resource that ensures our project gets build every 60 seconds - could be smarter.
        - name: time
          type: time
          source:
            interval: 60s
	``` 

6.  We have added all the resources we need for our build.  Time to tell Concourse how to use those resources to do something useful.  Add the following at the bottom of '**pipeline.yml**'

	```
		#The things that we want our concourse server to do.
		jobs:
		#User maven to compile our code, run test and make a JAR.
		  - name: scheduled-maven-package
		    plan:
		    - aggregate:
		      - get: git-repo
		        trigger: true
		      - get: time
		    - task: maven-package
		      privileged: true
		      file: git-repo/build/task-maven-package.yml
	```
This defines the jobs in our pipeline.  In our case we only have on job.  Each job has some top level properties, such as its name, which is shown in the Concourse UI and a plan.  The plan is the container for all the things that the job should do.  

	In our case we have added an '**aggregate**' element which runs its child elements in parallel.  If both child elements are successful then the plan continues.  We are performing a '**get**' on our '**git-repo**' resource that we defined in *step 4* above.  The other element in the aggregate is the **time** element.  Note that the '**get: git repo**' element has a **'trigger:  true'** property which will trigger the build if a new version of the resource exits.  So, in short, this build will continue if, at the turn of every 60 seconds there is a new version of content in our git repository.

	The next task in the plan is to actually compile our code by using maven with some parameters.  This is slightly complicated so rather than put all of the detail in the main pipeline we have chosen to define the task in a seperate file which we could reuse in other jobs if we needed.  We have used the '**privileged**' property to ensure the task runs under the root user.
	
That is actually all that is in the main pipeline.  Next we need to take a look at our '**task-maven-package.yml**' file to see what that is doing.

###The Maven Task
Tasks are designed to be totally isolated and idependent units of work.  This isolation extends down to the environment they are run in.  Each task can be associated with a container that host the task, which gets blown away when the task is finished.

For our example this means that we can spin up a docker container that has Java 8 and Maven installed on it.  That will get passed our '**git repo**' resource and we can execute some command line commands to build, test and package our project.

1.  Ensure you are in the '**build**' directory we created earlier.

2.  Create a file to hold our task definition.  In the job we defined in *step 6* above you can see that we decided to call the file **task-maven-package.yml**, so we better make that file:

	```
		touch task-maven-package.yml
	```
Note that task files do not have to be named 'task-...' but it seemed like a good idea.

3.  Edit the '**task-maven-package.yml**' we just created with your favourite text editor.

	```
		nano task-maven-package.yml
	```
	
4.  Add the following content to '**task-maven-package.yml**':

	```
	---
	platform: linux

	#define the base docker image used to build
	#the container that will host this task while it executes.
	#Alpine Linux with Java 8 and Maven 3.3
	image_resource:
	  type: docker-image
	  source:
	    repository: andreptb/maven
	    tag: 'latest'

	inputs:
	  - name: git-repo

	#execute mvn package against the target pom
	run:
	  path: mvn
	  args: ["-f", "git-repo/pom.xml", "-DskipTests=false", "package"]
	```
	
The **'platform'** element defines what sort of platform the task should be run on.  Not entirely sure of the impact of this element.

The **'image-resource'** element allows you to specify the base image of the container used to host the task as it executes.  We've chosen to use a docker image that is based on Alpine Linux (nice and small), has Java 8 installed and also has Maven installed.  All the essentials for compiling and packaging our Java applicaiton.

The task has one input, the '**git-repo**' resource we defined earlier in '**pipeline.yml**'.

The '**run**' element is where we actually do the work.  This allows us to essentially specify what we want to run on the command line.  In our case we would like to do 

```
	mvn -f git-repo/pom.xml -DskipTests=false package
```
	
If you know your maven says that we want to package the application defined in the top level pom of our git-repo resource and we want to make sure the tests are run.


###Executing our Build Pipeline
Now that we have defined our build pipeline we need to submit it to Concourse before we can do anything with it.  

1.  Ensure you are in the '**build**' directory we created earlier.

2.  Commit it to the concourse server with:

	```
	fly -t myconcourse set-pipeline -p simple-java-build-pipeline -c pipeline.yml
	```
	**-t** specifies the target Concourse.  We use the convienient alias we setup earlier.
	
	**-p** specifies the name we want to give our newly created pipeline.
	
	**-c** specifies the content file that we want to upload.

3.  You'll be asked if you want to apply the configuration.  Enter **'Y'**.

4.  Open your web browser and navigate to [the Concourse web UI](http://192.168.100.4:8080/).  You should see your new pipeline listed.

	![Concourse Pipeline UI](/Users/IWC-NeilDunlop/Desktop/Screen Shot 2016-07-29 at 23.34.47.png)

5.  Clicking on the icon at the top left of the screen will open the Pipeline List and show all the pipelines that are currently registered with the server.  Clicking the '**play/pause**' icon to the left of our pipeline will start the pipeline and update the display.

	![Concourse Pipeline Job Started](/Users/IWC-NeilDunlop/Desktop/Screen Shot 2016-07-29 at 23.36.40.png)

6.  Clicking on the **scheduled-maven-package** job in the main workspace will give you access to the running details of the job:

	![Concourse Pipeline Job Running](/Users/IWC-NeilDunlop/Desktop/Screen Shot 2016-07-29 at 23.36.56.png)

7.  Once the job completes, everything goes a lovely green:

	![Concourse Pipeline Job Detail Finished](/Users/IWC-NeilDunlop/Desktop/Screen Shot 2016-07-29 at 23.38.39.png)

	![Concourse Pipeline Job Started](/Users/IWC-NeilDunlop/Desktop/Screen Shot 2016-07-29 at 23.38.54.png)
 