log-parser-plugin
=================

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Flog-parser-plugin%2Fmaster)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Flog-parser-plugin/branches/)

This project based on [log-parser-plugin](https://wiki.jenkins.io/display/JENKINS/Log+Parser+Plugin), but changed configuration with project workspace file path to rule text.

### example
![项目配置私有解析规则](https://github.com/youshutong2080/log-parser-plugin/raw/master/images/project_rule_configure.png)       
![配置通用解析规则](https://github.com/youshutong2080/log-parser-plugin/raw/master/images/system_rules_configure.png)        
![项目配置能用解析规则](https://github.com/youshutong2080/log-parser-plugin/raw/master/images/select_global_rule_configure.png)       

### configure rules text

#### Format
Each line in the textarea specifies a level (ok/error/warn/info/start) and a regular expression (based on java.util.regex.Pattern) delimited by slashes ("/") to look for in order to mark the line as matching that level.

1. ok/error/warn           
Used to identify problem lines.
1.1 info         
These lines are highlighted blue in the report. They are used to create a set of links into the report for quick access to certain sections.
1.2 start          
Like info lines, they are highlighted blue in the report, and appear in the set of quick access links to the report. In addition, they are used for grouping the list of errors and warnings found in that section.
For example, the following line in the parsing rules file means that a line including the word "ERROR" will be marked as an error line:
```text
error /ERROR/
```
1.3 Case insensitive matches        
The notation supports the Java regex embedded flag expression. To perform a case-insensitive match, use (?i) notation.
Example: to match lines starting with the case-insensitive word 'error', use:
```text
error /(?i)^error /
```
1.4 If a line matches several rules, the first one applies.          
So for the following set of rules :
```text
ok /^javadoc:/
error /Error/
```
And the following line in the build log:
```text
javadoc: Error : could not find something
```
The line will be considered "ok", and will not be highlighted as an error, since the ^javadoc: pattern is matched first.         

1.5 Lines starting with a '#' character, and blank lines will be ignored to allow comments and spacing.
Example parsing rules file:
```text
ok /not really/

 
# match line starting with 'error ', case-insensitive
error /(?i)^error /
 
# list of warnings here...
warning /[Ww]arning/
warning /WARNING/
 
# create a quick access link to lines in the report containing 'INFO'
info /INFO/
 
# each line containing 'BUILD' represents the start of a section for grouping errors and warnings found after the line.
# also creates a quick access link.
start /BUILD/
```

### Run
```bash
# run default with 8080 and access url: http://localhost:8080/jenkins
mvn hpi:run 
# If you need to launch the Jenkins on a different port than 8080, set the port through the system property jetty.port.
mvn hpi:run -Djetty.port=8090
# maven-hpi-plugin 1.65 or later (used by parent POM 1.401 or later) can set the context path by using a system property. On more recent versions of Jenkins, the "/jenkins" prefix is added automatically.
mvn hpi:run -Dhpi.prefix=/jenkins
```

### Build
```bash
mvn package
```


