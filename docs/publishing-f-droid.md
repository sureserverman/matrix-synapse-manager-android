# Publishing Matrix Synapse Manager to F-Droid

This guide explains **step by step** how to get the app into [F-Droid](https://f-droid.org/) so users can install and update it from the F-Droid app or website.

---

## What F-Droid is and how it works

- **F-Droid** is a store for free and open source Android apps. Users install the F-Droid app (or use the website) and download your app from there instead of Google Play.
- F-Droid **does not use your APK**. They **build the app from your source code** on their servers and sign it with their own key. So they need:
  - A **public** Git repository (GitHub, GitLab, etc.) that anyone can clone.
  - **Metadata** (a YAML file) that describes your app and how to build it. That metadata lives in a separate repo called **fdroiddata**.

You either **ask F-Droid to add the app** (they write the metadata and build it) or **you add the metadata yourself** and send it as a merge request. This guide covers both.

---

## Words used in this guide

- **fdroiddata** — A GitLab repository ([gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata)) that holds one YAML file per app. That file describes the app (name, description, license, repo URL) and how to build it (e.g. Gradle, which tag to use).
- **RFP (Request for Packaging)** — A place where you open an **issue** to say “please add my app.” A volunteer (“packager”) will add the metadata and get the build working.
- **Merge request (MR)** — Like a pull request on GitHub. You fork fdroiddata, add the YAML file yourself, then open an MR so F-Droid can merge it.
- **Tag** — A Git tag (e.g. `v1.0.0`) that marks a specific commit as a release. F-Droid uses tags to know which version to build and when a new version exists.

---

## What you need before starting

1. **Public Git repository**  
   Your app must be in a repo that anyone can clone (e.g. [https://github.com/sureserverman/matrix-synapse-manager-android](https://github.com/sureserverman/matrix-synapse-manager-android)). F-Droid will clone it and build; they do not accept a pre-built APK for the main repo.

2. **At least one release tag**  
   F-Droid needs a tag (e.g. `v1.0.0`) that matches the version in the metadata. If you haven’t already:

   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

   For future releases you’ll create new tags (e.g. `v1.1.0`) and push them; F-Droid can then pick up new versions automatically.

3. **Metadata and store copy in this repo**  
   - **F-Droid metadata (build recipe):** [docs/f-droid/com.matrix.synapse.manager.yml](f-droid/com.matrix.synapse.manager.yml) contains license, repo URL, and build instructions. Copy this file into fdroiddata as `metadata/com.matrix.synapse.manager.yml`.  
   - **Summary and description:** F-Droid pulls them from the app repo via the [fastlane structure](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/): [fastlane/metadata/android/en-US/](fastlane/metadata/android/en-US/) holds `short_description.txt`, `full_description.txt`, `title.txt`, and changelogs. Do not add Summary or Description to the fdroiddata YAML.
   - **Icon (and optional screenshots):** The website and client need **`fastlane/metadata/android/en-US/images/icon.png`**. Without it, the app page has no icon. Optional: `images/phoneScreenshots/*.png`, `images/featureGraphic.png` — see the linked F-Droid doc. F-Droid reads metadata from the **latest tagged release**, so after adding assets you need a **new tag** (and push) for the build to pick them up.

---

## Option A: Ask F-Droid to add the app (easiest)

You open an **issue** and a volunteer adds the app for you. You don’t need to edit the fdroiddata repo yourself.

1. Open the **Request for Packaging** issue tracker in your browser:  
   **https://gitlab.com/fdroid/rfp/-/issues**

2. Click **“New issue”** (or “Create issue”).

3. Choose the **“Request for Packaging”** template if the site shows templates. That gives you a form with fields to fill.

4. Fill in the fields clearly:
   - **App name:** `Matrix Synapse Manager`
   - **Package name (application ID):** `com.matrix.synapse.manager`
   - **Source code URL:** `https://github.com/sureserverman/matrix-synapse-manager-android`
   - **License:** `Apache-2.0`
   - **Short description:** e.g. “Admin panel for Matrix Synapse homeservers. Manage users, rooms, media, federation and server health.”

5. In the issue text, add a link to your metadata file so the packager can use it:  
   `https://github.com/sureserverman/matrix-synapse-manager-android/blob/main/docs/f-droid/com.matrix.synapse.manager.yml`  
   You can write something like: “Metadata draft is here: [link].”

6. Submit the issue.

**What happens next:** A volunteer (packager) will create the app entry in fdroiddata, set up the build, and get it building. This can take from a few days to a few weeks depending on how busy they are. You don’t need to do anything else unless they ask you a question in the issue.

---

## Option B: Add the app yourself with a merge request

You add the metadata file to the fdroiddata repo and send a merge request. The pipeline in your fork may fail if you haven’t verified your GitLab identity; that’s OK — F-Droid can still merge your MR and build on their side.

### Step 1: GitLab account and fork

1. If you don’t have one, create an account at **https://gitlab.com**.
2. Open the F-Droid data repository: **https://gitlab.com/fdroid/fdroiddata**.
3. Click **“Fork”** (top right). This creates a copy of the repo under your account (e.g. `https://gitlab.com/your-username/fdroiddata`).

### Step 2: Create a branch and add the metadata file

1. In **your fork** of fdroiddata, click **“+”** or **“Code”** and create a **new branch**. Name it something clear, e.g. `com.matrix.synapse.manager`. Base it on `master` (or the default branch shown).
2. Open the **`metadata`** folder in your fork. That folder contains one `.yml` file per app (e.g. `org.example.app.yml`).
3. Click **“New file”** (or “+” then “New file”). Set the file path to **`metadata/com.matrix.synapse.manager.yml`** (the filename must be the app’s package name + `.yml`).
4. Open the metadata file from this repo: [docs/f-droid/com.matrix.synapse.manager.yml](f-droid/com.matrix.synapse.manager.yml). Copy **all** its content.
5. Paste into the new file in GitLab. Do not add Summary or Description—F-Droid will use the app’s [fastlane metadata](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/) in this repo (`fastlane/metadata/android/en-US/`).

6. Commit the file (e.g. message: “Add Matrix Synapse Manager”) and push the branch to your fork.

### Step 3: Open a merge request

1. In your fork, GitLab will often show a banner like “Create merge request” for the branch you just pushed. Click it. Or go to **Merge requests** → **New merge request**.
2. Set **Source** to your fork and the branch you created (e.g. `your-username/fdroiddata` → `com.matrix.synapse.manager`).
3. Set **Target** to **fdroid/fdroiddata** and branch **master**.
4. Fill in the merge request template (title and description). You can write something like: “Add Matrix Synapse Manager – admin app for Synapse homeservers.”
5. Submit the merge request.

**What happens next:** F-Droid maintainers will review your MR. The CI pipeline might show as failed on your fork (e.g. if GitLab identity verification is required to run jobs); that does **not** block the MR. They will merge it if the metadata is correct and then build the app on their infrastructure. After the build succeeds, the app will appear in the F-Droid catalog. Timing is explained here: [F-Droid wiki – How long until my app shows up?](https://gitlab.com/fdroid/wiki/-/wikis/FAQ#how-long-does-it-take-for-my-app-to-show-up-on-website-and-client)

---

## After your app is in F-Droid

- **New versions:** When you release a new version (e.g. 1.1.0), create a Git tag (e.g. `v1.1.0`) and push it. The metadata is already set to use “Tags” for updates, so F-Droid will detect the new tag and can build and publish the new version automatically.
- **Badge:** You can add an F-Droid “Get it on F-Droid” or version badge to your README. Instructions: [F-Droid Badges](https://f-droid.org/docs/Badges/). Example version badge URL:  
  `https://img.shields.io/f-droid/v/com.matrix.synapse.manager.svg?logo=F-Droid`

---

## Summary checklist

- [ ] App is in a **public** Git repository: https://github.com/sureserverman/matrix-synapse-manager-android
- [ ] At least one **release tag** exists and is pushed (e.g. `v1.0.0`)
- [ ] You did **either**:
  - **Option A:** Opened an RFP issue at https://gitlab.com/fdroid/rfp/-/issues and filled in the template, **or**
  - **Option B:** Forked fdroiddata, added `metadata/com.matrix.synapse.manager.yml`, and opened a merge request to fdroid/fdroiddata
