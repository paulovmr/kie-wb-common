/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.library.client.settings.util.dropdown;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLUListElement;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.widgets.client.widget.ListItemPresenter;
import org.kie.workbench.common.widgets.client.widget.ListPresenter;
import org.uberfire.client.mvp.UberElemental;

public class KieDropdownElement<T> implements org.jboss.errai.common.client.api.elemental2.IsElement {

    private final View view;
    private final ItemsListPresenter<T> itemsListPresenter;

    Consumer<T> onChange;

    @Inject
    public KieDropdownElement(final View view,
                              final ItemsListPresenter<T> itemsListPresenter) {
        this.view = view;
        this.itemsListPresenter = itemsListPresenter;
        this.onChange = i -> {
        };
    }

    @PostConstruct
    public void init() {
        view.init(this);
    }

    public void setup(final List<Item<T>> items,
                      final String initialSelectedLabel,
                      final Consumer<T> onChange) {

        itemsListPresenter.setup(
                view.getUl(),
                items,
                (item, presenter) -> presenter.setup(item, this));

        view.setSelectedLabel(initialSelectedLabel);

        this.onChange = onChange;
    }

    public void setValue(final String value) {
        view.setSelectedLabel(value);
    }

    @Override
    public HTMLElement getElement() {
        return view.getElement();
    }

    public interface View extends UberElemental<KieDropdownElement> {

        HTMLUListElement getUl();

        void setSelectedLabel(final String label);
    }

    public static class Item<T> {

        public final T value;
        public final String label;
        public final Status status;

        public Item(final T value,
                    final String label,
                    final Status status) {

            this.label = label;
            this.value = value;
            this.status = status;
        }

        public enum Status {
            CHECKED,
            UNCHECKED
        }
    }

    @Dependent
    public static class ItemElement<T> extends ListItemPresenter<Item<T>, KieDropdownElement<T>, KieDropdownElementView.Item<T>> {

        private Item<T> item;
        private KieDropdownElement<T> parentPresenter;

        @Inject
        public ItemElement(final KieDropdownElementView.Item<T> view) {
            super(view);
        }

        @Override
        public ItemElement<T> setup(final Item<T> item,
                                    final KieDropdownElement<T> parentPresenter) {
            this.item = item;
            this.parentPresenter = parentPresenter;

            view.init(this);
            view.setLabel(item.label);
            view.setStatus(item.status);

            return this;
        }

        @Override
        public Item<T> getObject() {
            return item;
        }

        public void onClick() {
            parentPresenter.onChange.accept(item.value);
        }
    }

    @Dependent
    public static class ItemsListPresenter<T> extends ListPresenter<Item<T>, ItemElement<T>> {

        @Inject
        public ItemsListPresenter(final ManagedInstance<ItemElement<T>> itemPresenters) {
            super(itemPresenters);
        }
    }
}

