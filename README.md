# QuPath Extension for Vectra Image Server
QuPath extension for opening multichannel TIFF files for Vectra microscope slides which are stored on a server with a specific backend API.
## Overview
The QuPath extension was made for the Computational Immunology Group at Radboud University and can only work to its full capacity given access to the lab's backend API. It was made to circumvent the need of downloading hundreds of tiles per Vectra slide to stitch together into one image to view in QuPath. Instead, the slide images are only ever stored on the server. With server access, users can open slide images as 3-channel JPEGs or full multichannel TIFFs directly in QuPath, view saved polygon and point annotations, and draw and save new polygons back to the server using QuPath's drawing tools.

## Requirements 
- QuPath v0.7.0.
- Access to the server
- Without access to the server, this extension WILL NOT BE USEFUL

## Installation
1. Build the extension's shadow jar (See [Build the extension](#build-the-extension)). 
2. Open QuPath v0.7.0, click on "Extensions > Manage extensions". Click on "Open Extension Directory". You'll be prompted to create a user directory if you don't already have one. A folder will then open. Drag the built jar from `build/libs/` into this folder. 
3. Restart QuPath. 

## Features 
- Open Vectra slide images stored on the lab's server. It is not necessary to first download all the tiles locally.
- Open images as 3-channel JPEG or as full multichannel TIFF format.
- Load and display saved polygon or point annotations from the server.
- Use QuPath's drawing tools to draw polygon annotations and save them to the server.

## Build the extension

Building the extension with Gradle should be pretty easy - you don't even need to install Gradle separately, because the 
[Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) will take care of that.
Open a command prompt, navigate to where the code lives, and use
```bash
gradlew build shadowJar
```

The built extension should be found inside `build/libs`.
See [Installation](#installation) above for how to add it to QuPath.
## License