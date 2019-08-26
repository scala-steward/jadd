# jadd

[ ![GitHub release](https://img.shields.io/github/release/d10xa/jadd.svg)](https://github.com/d10xa/jadd/releases)
[ ![Travis](https://img.shields.io/travis/d10xa/jadd.svg)](https://travis-ci.org/d10xa/jadd)
[ ![Coveralls github](https://img.shields.io/coveralls/github/d10xa/jadd.svg)](https://coveralls.io/github/d10xa/jadd)


Tool for adding dependencies to gradle/maven/sbt build files

[![jadd.gif](data/jadd.gif)](https://github.com/d10xa/jadd#usage "d10xa/jadd")

## REPL!

Just run `jadd` without arguments and enjoy tab completion!

## usage

    jadd i logback-classic postgresql gson commons-io io.grpc:grpc-protobuf
    jadd i -r jrequirements.txt

## commands

- `install` (shortcut `i`) add dependency to build file

- `search` (shortcut `s`) print dependency to console

- `show` show artifacts from build file

- `help`

## installation

    curl -s https://raw.githubusercontent.com/d10xa/jadd/master/install.sh | bash

And then add following to `~/.profile` or `~/.bashrc` or `~/.zshrc`

    export PATH=$PATH:$HOME/.jadd/bin

## update

just run installation script

## examples

### maven

    mvn archetype:generate -DgroupId=com.example -DartifactId=example-mvn -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
    cd example-mvn
    jadd i logback-classic

### gradle

    mkdir example-gradle
    cd example-gradle
    gradle init --type java-application
    jadd i mysql

### sbt

    sbt new scala/scala-seed.g8
    # name [Scala Seed Project]: example-sbt
    cd example-sbt
    jadd i akka-http
