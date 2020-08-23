Site-aware LDAPS Provider
=========================

Provides LDAPS site awareness.

 * [RFC 2782](https://tools.ietf.org/html/rfc2782) only mentions LDAP and not LDAPS
 * Active Directory per default does not create SRV DNS records
 * site-awareness is a proprietary Active Directory feature

[JDK-8192975](https://bugs.openjdk.java.net/browse/JDK-8192975)

Currently requires the JNDI DNS client and Java 12.

Usage
-----

Put the JAR in the classpath or module path.


```xml
<dependency>
  <groupId>com.github.marschall</groupId>
  <artifactId>site-aware-ldaps-provider</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
Hashtable<String, String> env = new Hashtable<>();
env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
env.put(Context.PROVIDER_URL, "ldaps:///dc=example,dc=com"); // ldap also supported
env.put(ActiveDirectoryContext.SITE, "site-name"); // optional
DirContext ctx = new InitialDirContext(env);
```

WildFly
-------

Dropping the JAR directly in `$JBOSS_HOME/modules` should add it to the Java module path (`-mp`)