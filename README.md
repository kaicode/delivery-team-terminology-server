Delivery Team Terminology Server
================================

A terminology server in development by the delivery team devs.

# Aims
This server aims to support browsing and authoring of multiple versions of the snomed product whilest being lightning fast and requiring only modest hardware.

## Persistence / Multiple work streams
This server works with highly denormalised representations of snomed concepts in json format, as produced by the rf2-to-json-conversion project. This alows concepts to be served along with all descriptions and related concepts at the fastest posible rate.

Modifications to conponents are stored as json deltas. Any number of branches may be created to store sets of modifications. When browsing a chosen branch the deltas are applied over the master version.
