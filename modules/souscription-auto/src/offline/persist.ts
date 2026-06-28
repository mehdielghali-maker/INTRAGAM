// Stockage PERSISTANT : demande au navigateur de ne pas évincer les données (photos/documents).
export async function demanderPersistance(): Promise<boolean> {
  if (!navigator.storage?.persist) {
    return false;
  }
  if (await navigator.storage.persisted()) {
    return true;
  }
  return navigator.storage.persist();
}
