# prom-helpers
Process mining utility classes, particularly when working with the [ProM](http://www.promtools.org/doku.php) and [OpenXES](http://www.xes-standard.org/openxes/start) frameworks.

# Features

## Headless console helpers

Run ProM plugins headless, by using a console-based `PluginContext`. Available in both UIPluginContext and non-UIPluginContext varieties.

```java
PluginContext uipc = 
    new HeadlessDefinitelyNotUIPluginContext(
               new ConsoleUIPluginContext(), "spn_dot_converter");	
```


## Petri net fragment parser

Simplify unit tests by specifying Petri nets in a simple ASCII visual syntax.

```java
// This is equivalent to a single net
//     [a] 
// I -/   \-> F
//    \[b]/
parser.addToNet(net, "I -> [a] -> F");
parser.addToNet(net, "I -> [b] -> F");
```

See detailed doc comment in [PetriNetFragmentParser](src/main/java/qut/pm/prom/helpers/PetriNetFragmentParser.java), which has more detail, and a grammar. There's also this [explanatory blog post](https://adamburkeware.net/2021/05/20/petri-net-fragments.html).

A command line interface which will convert Petri net fragments (.frag) to PNML is exposed in `PetriNetConverter.main()`.

```
usage: pnc [-i <arg>] [-o <arg>] [-v]
 -i,--input-format <arg>    Input format (PNML,FRAG)
 -o,--output-format <arg>   Output format (PNML,DOT)
 -v,--verbose               Verbose output.
```

## XES parser for delimited text files

Convenience parser for text delimited files. For testing especially, these are terser and quicker to understand than the verbosity and genericity of full XES files. Each argument is a trace and each delimited entry is an event. See [DelimitedTraceToXESConverter](src/main/java/qut/pm/xes/helpers/DelimitedTraceToXESConverter.java).

```java
DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter(); 
XLog log = converter.convertTextArgs("a b d",
                                     "a b d",
                                     "a b d",
                                     "a c d");
```


# Building

`./gradlew test`

`prom-helpers` is built with gradle and compatible with ivy, which is heavily used for ProM projects.
 
# Using As Library

The ProM package system places some limitations on publishing standalone maven packages (specifically the lack of a maven-compatible versioned jar) and this library is not fully maven-compatible as a result. Ivy repository configuration for gradle can be found in the `build.gradle` file. Using ivy in the consuming project should also work.
