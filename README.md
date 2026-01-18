# WINSOME: a reWardINg SOcial Media

**Corso:** Laboratorio di Reti
**Progetto di fine corso:** A.A. 2021/22, versione 2

WINSOME è un social media che si ispira a STEEMIT, una piattaforma basata su blockchain che premia gli utenti per contenuti interessanti e i curatori che commentano o votano tali contenuti. A differenza di STEEMIT, WINSOME utilizza un’architettura client-server centralizzata e un meccanismo di ricompense semplificato, con valuta interna denominata **wincoin**.

---

## Contesto

* Gli utenti possono registrarsi, fare login, seguire altri utenti e creare contenuti.
* Il feed mostra i post degli utenti seguiti.
* Implementa un meccanismo di **“rewin”**, simile al retweet, per condividere post sul proprio blog.
* I post possono ricevere **voti positivi e negativi** e commenti.
* Le **ricompense** sono suddivise tra autore del post e curatori (chi vota/commenta positivamente).

---

## Architettura del Sistema

### Componenti principali

* **WinsomeClient**

  * Interfaccia con l’utente (GUI)
  * Invia comandi al server tramite **TCP**
  * Riceve notifiche su aggiornamenti portafoglio tramite **UDP multicast**
  * Registro e aggiornamento follower tramite **RMI Callback**

* **WinsomeServer**

  * Gestisce registrazione utenti, autenticazione e memorizzazione dati
  * Calcola periodicamente le ricompense
  * Aggiorna i portafogli degli utenti
  * Gestisce comunicazioni con client via TCP, RMI e UDP multicast
  * Mantiene lo stato persistente in file **JSON** (utenti, post, wallet)

---

## Funzionalità principali

### Gestione utenti

* `register(username, password, tags)` – Registrazione con massimo 5 tag
* `login(username, password)` – Login utente
* `logout()` – Logout
* `listUsers()` – Lista utenti con tag in comune
* `listFollowers()` – Lista dei follower
* `listFollowing()` – Lista degli utenti seguiti
* `followUser(idUser)` – Seguire un utente
* `unfollowUser(idUser)` – Smettere di seguire un utente

### Gestione post

* `createPost(title, content)` – Creare un nuovo post (max 20 char titolo, 500 char contenuto)
* `showFeed()` – Mostra feed con post degli utenti seguiti
* `showPost(idPost)` – Mostra dettagli di un post, voti e commenti
* `deletePost(idPost)` – Cancella un post (solo autore)
* `rewinPost(idPost)` – Condividere un post nel proprio blog
* `ratePost(idPost, vote)` – Assegnare voto positivo (+1) o negativo (-1)
* `addComment(idPost, comment)` – Commentare un post

### Portafoglio e ricompense

* `getWallet()` – Visualizza portafoglio e storico incrementi
* `getWalletInBitcoin()` – Converte wincoin in bitcoin usando tasso casuale (RANDOM.ORG)
* Calcolo ricompense periodico basato su voti e commenti:

  * Ricompensa Autore: basata sul gradimento del post
  * Ricompensa Curatore: basata su voti positivi e commenti
  * Percentuale configurabile tra autore e curatori
  * Decadimento del valore del post con il tempo

---

## Architettura Tecnica

* **Client-Server Java**

  * TCP persistente per comandi
  * RMI per registrazione e callback follower
  * UDP multicast per notifiche portafoglio
* **Threading**

  * Server: thread principale + thread daemon per calcolo ricompense + thread worker per ogni client
  * Client: thread per ascolto multicast (daemon)
* **Persistenza dati**

  * File JSON: `utenti.json`, `posts.json`, `wallet.json`
  * Stato ricostruito all’avvio del server
* **Librerie**

  * `gson-2.8.9.jar` per serializzazione JSON

---

## Compilazione ed Esecuzione

### Compilazione

```bash
javac -cp ./gson-2.8.9.jar *.java
```

### Esecuzione Server

```bash
java -cp ./gson-2.8.9.jar WinsomeServer
```

### Esecuzione Client

```bash
java WinsomeClient
```

> Nota: Il server deve essere avviato prima dei client.

---

## Struttura dei File GSON

* **User**

  * `nickname`, `password`, `tag1`-`tag5`, `following`, `followers`, `feed`
* **Post**

  * `idPost`, `autore`, `n_iterazioni`, `titolo`, `textBody`, `commenti`, `voti`
* **Wallet**

  * `proprietario`, `portafoglio`, `portafoglio_storico`, `timestamp_storico`

---

## Interfaccia Client

* Interfaccia grafica con input area per username, password e tag
* Bottoni per registrazione, login, gestione followers, creazione post, voting, rewin, portafoglio
* Aggiornamenti feed e portafoglio ricevuti in tempo reale tramite RMI callback e UDP multicast

---

## Threading e Concorrenza

* **Server**

  * Thread worker per ogni client (gestione richiesta fino a logout)
  * Thread daemon per timer e calcolo ricompense

* **Client**

  * Thread aggiuntivo daemon per ascolto multicast

* **Sincronizzazione**

  * Metodi critici dichiarati `synchronized`
  * Nessun uso di `wait()` o `notify()`

---

## Materiale consegnato

* Codice sorgente Java
* Libreria esterna `gson-2.8.9.jar`
* Relazione PDF con architettura, thread, classi, istruzioni di compilazione ed esecuzione
