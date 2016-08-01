Improving our Concourse Pipeline
================================

While our existing Concourse Pipeline does the basics reasonably well, there are a few things that we can change to make it more flexible, more maintainable and more fully featured.

Externalise Variable Information
--------------------------------

Pipelines often contain information that can change from instance to instance, particularly if we want to reuse the pipeline to build other instances of our application in different environments.
A simple example of this is that we might want to host our code in a different repository or build a different branch of our code in our chosen repository.

Concourse supports the use of a '**properties file**' to hold values for variables.  For example we can place our github repository details in the properties file.

   
    github-uri: https://github.com/.../simplejava.git
    github-branch: master
    

Updating our Pipeline
---------------------

We'll make a really simple change to your pipeline, just to demonstrate how properties are set and used.  All we will do is move our github url and branch name to a new properties file.

	
1.  Ensure you are in the '**build**' directory we created earlier.

2.  Create a new directory call '**properties**' to hold our properties files and navigate into the new directory.

    ```
    mkdir properties
    cd properties
    ```

3.  Create a file to hold the properties we want to externalise.  Give it a meaningful name so we know what project it applies to and what properties it holds.

	```
	touch simplejava_github_properties.yml
	```

4.  Edit the '**simplejava_github_properties.yml**' we just created with your favourite text editor.

	```
	nano simplejava_github_properties.yml
	```
	
5.  We'll just add two properties.  One for github repository url and one for the branch we want to build.  Add the following content to '**simplejava_github_properties.yml**':

	```
	github-uri: https://github.com/neildunlop/simplejava.git
   github-branch: master
	```

6.  Now we'll edit our pipeline to use these new properties.  Ensure you are in the build directory and edit the '**pipeline.yml**' file with your favourite editor.

    ```
    nano pipeline.yml
    ```

7.  Locate the existing `source` element for our `git-repo` resource and update the `uri` property it to be:

	```
	uri: {{github-uri}}
	```
	
8.  Locate the `branch` property for the same element and update the value to be:

	```
	branch: {{github-branch}}
	```

###Executing our Build Pipeline
Now that we have enhanced our build pipeline we need to submit it to Concourse before we can do anything with it.  

1.  Ensure you are in the '**build**' directory we created earlier.

2.  Commit it to the concourse server with:

	```
	fly -t myconcourse set-pipeline -p simple-java-build-pipeline -c pipeline.yml -l ./properties/simplejava-github-properties.yml
	```
	**-t** specifies the target Concourse.  We use the convienient alias we setup earlier.
	
	**-p** specifies the name we want to give our newly created pipeline.
	
	**-c** specifies the content file that we want to upload.
	
	**-l** or **--load-vars-from** specifies the value should be the path the properties file you want to use.

3.  You'll be asked if you want to apply the configuration.  Enter **'Y'**.

4.  Open your web browser and navigate to [the Concourse web UI](http://192.168.100.4:8080/).  You should see your new pipeline listed.
