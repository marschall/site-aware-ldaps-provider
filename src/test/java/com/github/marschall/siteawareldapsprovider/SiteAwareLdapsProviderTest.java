package com.github.marschall.siteawareldapsprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.junit.jupiter.api.Test;

class SiteAwareLdapsProviderTest {
  
  @Test
  void toServiceName() throws NamingException {
    assertEquals("_ldap._tcp.example.com", SiteAwareLdapsProvider.toServiceName("ldaps:///dc=example,dc=com"));
  }

  @Test
  void test() throws NamingException {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap:///dc=example,dc=com");
    DirContext ctx = new InitialDirContext(env);
  }

}
