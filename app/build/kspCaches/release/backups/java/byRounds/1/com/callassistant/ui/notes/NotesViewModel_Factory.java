package com.callassistant.ui.notes;

import com.callassistant.data.repository.ContactRepository;
import com.callassistant.data.repository.NotesRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class NotesViewModel_Factory implements Factory<NotesViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<NotesRepository> notesRepositoryProvider;

  public NotesViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<NotesRepository> notesRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.notesRepositoryProvider = notesRepositoryProvider;
  }

  @Override
  public NotesViewModel get() {
    return newInstance(contactRepositoryProvider.get(), notesRepositoryProvider.get());
  }

  public static NotesViewModel_Factory create(Provider<ContactRepository> contactRepositoryProvider,
      Provider<NotesRepository> notesRepositoryProvider) {
    return new NotesViewModel_Factory(contactRepositoryProvider, notesRepositoryProvider);
  }

  public static NotesViewModel newInstance(ContactRepository contactRepository,
      NotesRepository notesRepository) {
    return new NotesViewModel(contactRepository, notesRepository);
  }
}
