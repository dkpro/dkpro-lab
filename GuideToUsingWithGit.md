# Guide to Using DKPro Lab with Git and Eclipse #



This is an introductory guide to using DKPro Lab with Git and Eclipse 4.3.1.

## One time preparation ##

We recommend installing `m2e-egit`, an Eclipse plug-in which add the option "Import Maven Projects..." to the context menu in the git repository view.

  1. In Eclipse, go to `Window` -> `Preferences` -> `Maven` -> `Discovery`.  Open `Catalog`.  Search for "m2e-egit".  Select it and Finish.
  1. Accept everything as default, agree to license, click Ok.  Keep installing unsigned content.
  1. Restart Eclipse when directed.

## Create your local clone ##

First, you need to create a clone of the remote repository on your local machine.

  1. Open Eclipse Git Repository Perspective.  Click "clone a git repository".
  1. In a browser, go to the DKPro Lab Google Code page, then `Source`.  Copy the "git clone" address, and paste it into the Eclipse `Clone Location URI`.  Other fields should auto-fill.  You will also need to get your Google Code password (from the GC DKPro Lab page, if you are signed in), and enter it here.  Username does not need to be your entire gmail address, which is different from when you commit svn to GC.  Use all other default options.
  1. Check out both "develop" and "master". Click `Next`
  1. Change `Initial branch` to `develop`.  This is the branch where your commits will go. Click `Finish`.

Now, DKPro Lab should be listed in your Eclipse Git Repository.  You have made a local clone and have also checked out a branch to work on.  The next step is to make the java side of Eclipse aware of the local clone and checked-out branch's existence.

Then you make a Maven copy for your Package Explorer.

  1. In Eclipse's Git Repository Perspective, open DKPro Lab, open `Working Directory`, right-click `de.tudarmstadt.ukp.dkpro.lab`, "Import Maven Projects..."
  1. Optionally, add it to the working set of your choice, then click Finish.

Congradulations!  You are all set to begin developing DKPro lab.

## Update your project ##

  1. Go to Git Repository Perspective, right-click, "pull."  This is just like svn update.  Now your local clone and your checked out branch are both updated and you are all set.

## Commit your work ##

When you are ready to merge your contributions with the main project, you can either commit entire files at once, or sets of changes from those files.

#### To commit entire files ####

  1. Right-click on the Package Explorer files with your changes -> `Team` -> `addToIndex`.  Then the snowflake icon appears.
  1. Right-click on files with the changes -> `Team` -> `Commit`.  Add a commit message and click on the files you want to include.  Then, commit and push.  This is just like svn commit.

#### To commit individual changes ####
  1. Go to Git Repository Perspective, then on the bottom of the screen go to `Git Staging`.  Click on your file.  It opens in a compare view.
  1. In between the 2 versions, click the arrow button for the changes you want to commit.  The changes are placed in the `index view`.
  1. Save this editor (Ctrl+s).
  1. Add a commit message, then commit and push.

#### Committing with remote changes ####

When you want to commit but someone else has committed since you last updated, you must stash your changes, update your local clone, and then replace your copy with the stash.  Git will not allow you to push your changes to the remote repository otherwise.

  1. Go to Git Repository Perspective.  Right-click on DKPro Lab -> "stash changes".  Call it "temp" or whatever.  Now, our local changes on the checked-out branch are gone, and saved in the Git Repository, in DKPro Lab, in "Stashed Commits."
  1. Right-click on project -> "pull."
  1. Right-click on your stashed commit("temp") -> "apply stashed changes."  Then our changes are back on our checked-out branch.  Delete the stashed copy.
  1. Now you are ready to commit.