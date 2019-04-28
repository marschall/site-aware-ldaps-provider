Site aware LDAPS Provider
=========================

Uses LDAP site awareness to provide LDAPS site awareness.

Usually no SRV records are provided for LDAPS, only for LDAP.

[JDK-8192975](https://bugs.openjdk.java.net/browse/JDK-8192975)

Currently requires the JNDI DNS client and Java 12.

WildFly
-------

Dropping the JAR directly in `$JBOSS_HOME/modules` should add it to the Java module path (`-mp`)