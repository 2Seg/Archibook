package com.isep.archibook.controllers;

import com.isep.archibook.services.LdapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.naming.NamingException;

@Controller
public class FixturesController {

    @Autowired
    public LdapService ldapService;

    @RequestMapping(name = "/fixtures", method = RequestMethod.GET)
    public ModelAndView fixtures(ModelAndView modelAndView) throws NamingException {

        ldapService.udpateDb(ldapService.getUsers("isep"));
        ldapService.udpateDb(ldapService.getUsers("audencia"));
        ldapService.udpateDb(ldapService.getUsers("polytechnique"));

        modelAndView.setViewName("login");
        return modelAndView;
    }

}
