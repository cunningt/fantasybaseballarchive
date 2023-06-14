# fgdownloader

1. Install jbang : https://www.jbang.dev/documentation/guide/latest/installation.html

JBang is a utility that enables self-contained java programs.  

2. Copy authentication.properties-example to authentication.properties and add your fangraphs username and password.

3. Edit reports.properties - add any reports that you want downloaded.    The format of each line should be the name of the eventual filename=URL.

4. Edit projections.properties - add any projections that you want downloaded.     The format of each line should be the name of the eventual filename=URL.

5. jbang fgdownloader.java

6. Results will show up in the data directory.
