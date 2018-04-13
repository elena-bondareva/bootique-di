package io.bootique.di.spi;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Qualifier;

import com.google.inject.Binding;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.di.DIBootstrap;

/**
 * Implementation of {@link com.google.inject.Injector} that
 * uses {@link io.bootique.di.Injector} internally.
 */
public class InjectorAdapter implements com.google.inject.Injector {

    private final DefaultInjector bootiqueInjector;
    private final BinderAdapter adapter;
    private final List<io.bootique.di.Key<?>> eagerSingletons;

    public InjectorAdapter(Iterable<? extends Module> modules) {

        // Create customized injector
        eagerSingletons = new ArrayList<>();
        bootiqueInjector = (DefaultInjector) DIBootstrap.injectorBuilder(b -> b.bind(Injector.class).toInstance(this))
                .defaultNoScope()
                .enableDynamicBindings()
                .withProvidesMethodPredicate(m -> m.isAnnotationPresent(Provides.class))
                .withQualifierPredicate(c -> c.isAnnotationPresent(Qualifier.class)
                        || c.isAnnotationPresent(BindingAnnotation.class))
                .withInjectAnnotationPredicate(o -> o.isAnnotationPresent(Inject.class)
                        || o.isAnnotationPresent(javax.inject.Inject.class))
                .withSingletonPredicate(el -> el.isAnnotationPresent(Singleton.class)
                        || el.isAnnotationPresent(javax.inject.Singleton.class))
                .withProviderPredicate(t -> Provider.class.equals(t)
                        || javax.inject.Provider.class.equals(t))
                .withProviderWrapper(p -> (Provider<Object>) p::get)
                .build();

        // Guice -> Bootique adapters
        adapter = new BinderAdapter(bootiqueInjector.getBinder(), this);
        // Configure all modules manually
        modules.forEach(this::installModule);
        // Create eager singletons
        eagerSingletons.forEach(bootiqueInjector::getInstance);
    }

    public void installModule(Module module) {
        module.configure(adapter);
        bootiqueInjector.getProvidesHandler().bindingsFromAnnotatedMethods(module).forEach(p -> p.bind(bootiqueInjector));
    }

    @Override
    public void injectMembers(Object instance) {
        bootiqueInjector.injectMembers(instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Provider<T> getProvider(Key<T> key) {
        return () -> (T)bootiqueInjector.getProvider(DiUtils.toBootiqueKey(key)).get();
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type) {
        return () -> (T)bootiqueInjector.getProvider(type).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(Key<T> key) {
        return bootiqueInjector.getInstance(DiUtils.toBootiqueKey(key));
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return bootiqueInjector.getInstance(type);
    }

    @Override
    public <T> Binding<T> getExistingBinding(Key<T> key) {
        io.bootique.di.spi.Binding<T> bootiqueBinding = bootiqueInjector.getBinding(DiUtils.toBootiqueKey(key));
        if(bootiqueBinding == null) {
            return null;
        }

        return new Binding<T>() {
            @Override
            public Key<T> getKey() {
                return key;
            }

            @Override
            public Provider<T> getProvider() {
                return () -> bootiqueBinding.getScoped().get();
            }
        };
    }

    <T> void markAsEagerSingleton(io.bootique.di.Key<T> bootiqueKey) {
        eagerSingletons.add(bootiqueKey);
    }
}