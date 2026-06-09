package com.rpm.app.ui.feature.patients;

import com.rpm.app.data.repository.PatientRepository;
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
public final class PatientListViewModel_Factory implements Factory<PatientListViewModel> {
  private final Provider<PatientRepository> repoProvider;

  public PatientListViewModel_Factory(Provider<PatientRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public PatientListViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static PatientListViewModel_Factory create(Provider<PatientRepository> repoProvider) {
    return new PatientListViewModel_Factory(repoProvider);
  }

  public static PatientListViewModel newInstance(PatientRepository repo) {
    return new PatientListViewModel(repo);
  }
}
