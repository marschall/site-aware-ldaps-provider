package com.github.marschall.siteawareldapsprovider;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.github.marschall.siteawareldapsprovider.SiteAwareLdapsProvider.DnsRecord;

public class SrvLookup {


  public static void main(String[] args) throws NamingException {
    String serviceName = "_xmpp-server._tcp.gmail.com";
    lookupService(serviceName);
  }

  private static void lookupService(String serviceName) throws NamingException {
    Hashtable<String, String> env = new Hashtable<>(4);
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    env.put(Context.PROVIDER_URL, "dns:");
    DirContext context = new InitialDirContext(env);

    try {
      Attributes attribues = context.getAttributes(serviceName, new String[] { "SRV" });

      List<DnsRecord> records = new ArrayList<>(attribues.size());
      NamingEnumeration<?> services = attribues.get("srv").getAll();
      try {
        while (services.hasMore()) {
          String service = (String) services.next();
          System.out.println(service);
        }
      } finally {
        services.close();
      }
    } finally {
      context.close();
    }
  }

}
