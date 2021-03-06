package dchq.dbconnect.controller;

import dchq.dbconnect.model.NameDirectory;
import dchq.dbconnect.service.NameDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 11/22/2015
 */
@Controller
@RequestMapping("/names")
public class NameDirectoryController {
    @Autowired
    private NameDirectoryService nameDirectoryService;

    @Autowired
    private SolrTemplate solrTemplate;

    @RequestMapping("/getList.json")
    @ResponseBody
    public List<NameDirectory> getList() {
        return nameDirectoryService.getAllRows();
    }

    @RequestMapping(value = "/search.json", method = RequestMethod.POST)
    @ResponseBody
    public List<NameDirectory> doSearch(@RequestBody String text) {
        if (StringUtils.isEmpty(text) || solrTemplate == null) {
            return this.getList();
        }

        String queryString = String.format("firstName:*%s* OR lastName:*%s*", text, text);
        Query query = new SimpleQuery(new SimpleStringCriteria(queryString));
        return solrTemplate.queryForPage(query, NameDirectory.class).getContent();
    }

    @RequestMapping(value = "/autocomplete.json", method = RequestMethod.POST)
    @ResponseBody
    public List<String> doTypeahead(@RequestBody String text) {
        if (StringUtils.isEmpty(text) || solrTemplate == null) {
            return Collections.emptyList();
        }

        String queryString = String.format("firstName:*%s*", text);
        Query query = new SimpleQuery(new SimpleStringCriteria(queryString));
        List<NameDirectory> content = solrTemplate.queryForPage(query, NameDirectory.class).getContent();
        List<String> result = new ArrayList<String>();

        for (NameDirectory nd : content) {
            result.add(nd.getFirstName());
        }
        return result;
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public void add(@RequestBody NameDirectory nd) {
        Long id = nameDirectoryService.addNameDirectory(nd);
        NameDirectory saved = nameDirectoryService.getById(id);

        if (this.isUseSolr()) {
            solrTemplate.saveBean(saved);
            solrTemplate.commit();
        }
    }

    @RequestMapping(value = "/remove/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void remove(@PathVariable("id") Long id) {
        nameDirectoryService.deleteNameDirectoryById(id);

        if (this.isUseSolr()) {
            solrTemplate.deleteById(String.valueOf(id));
            solrTemplate.commit();
        }
    }

    @RequestMapping(value = "/removeAll", method = RequestMethod.DELETE)
    @ResponseBody
    public void removeAll() {
        nameDirectoryService.deleteAll();

        if (this.isUseSolr()) {
            solrTemplate.delete(new SimpleQuery(new SimpleStringCriteria("*:*")));
            solrTemplate.commit();
        }
    }

    @RequestMapping("/layout")
    public String getPartialPage() {
        if (this.isUseSolr()) {
            return "namedirectory/layout_search";
        }
        return "namedirectory/layout";
    }

    protected boolean isUseSolr() {
        return solrTemplate != null;
    }
}
