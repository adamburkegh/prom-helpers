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

Simplify unit tests by specifying unit tests in a simple ASCII visual syntax.

```java
// This is equivalent to a single net
//     [a] 
// I -/   \-> F
//    \[b]/
parser.addToNet(net, "I -> [a] -> F");
parser.addToNet(net, "I -> [b] -> F");
```

See detailed doc comment in [PetriNetFragmentParser](src/main/java/qut/pm/prom/helpers/PetriNetFragmentParser.java).

A command line interface which will convert Petri net fragments (.frag) to PNML is exposed in `PetriNetConverter.main()`.

```
usage: pnc [-i <arg>] [-o <arg>] [-v]
 -i,--input-format <arg>    Input format (PNML,FRAG)
 -o,--output-format <arg>   Output format (PNML,DOT)
 -v,--verbose               Verbose output.
```

# Building

`./gradlew test`

`prom-helpers` is built with gradle and compatible with ivy, which is heavily used for ProM projects.
 
# Using As Library

The ProM package system places some limitations on publishing standalone maven packages (specifically the lack of a maven-compatible versioned jar), so there is currently no standalone package available. `prom-helpers` can be built and installed with gradle locally.
