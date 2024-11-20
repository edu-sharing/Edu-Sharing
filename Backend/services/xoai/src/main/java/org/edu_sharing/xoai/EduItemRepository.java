package org.edu_sharing.xoai;

import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.filter.ScopedFilter;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.handlers.results.ListItemsResults;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.dataprovider.repository.ItemRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EduItemRepository implements ItemRepository {
    private final EduDataHandler handler;



    public EduItemRepository(EduDataHandler handler) {
        this.handler=handler;
    }

    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException, OAIException {
        return handler.getItem(identifier);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        return handler.getIdentifiers(offset,length, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return handler.getIdentifiersFrom(offset,length, from, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return handler.getIdentifiersUntil(offset,length, until, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return handler.getIdentifiersFromUntil(offset,length, from, until, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return handler.getIdentifiers(offset,length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return handler.getIdentifiersFrom(offset,length, from, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return handler.getIdentifiersUntil(offset,length, until, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return handler.getIdentifiersFromUntil(offset,length, from, until, setSpec);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        ListItemIdentifiersResult identifiers = getItemIdentifiers(filters, offset, length);
        List<Item> array=new ArrayList<>();
        for(ItemIdentifier identifier : identifiers.getResults()){
            try {
                array.add(getItem(identifier.getIdentifier()));
            } catch (IdDoesNotExistException e) {}
        }
        return new ListItemsResults(identifiers.hasMore(),array);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return null;
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return null;
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return null;
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItems(filters,offset,length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return null;
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return null;
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return null;
    }
}
