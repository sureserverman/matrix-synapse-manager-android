# Recent Localization Gaps

Scope: uncommitted changes compared with `HEAD` on 2026-05-02. I treated
Compose text, content descriptions, alert dialog copy, empty states, and snackbar
messages as user-facing interface. Test tags, route names, audit-log map keys,
API path strings, and Matrix identifiers are excluded.

## Summary

Translated resource folders checked:

- `values-de`
- `values-es`
- `values-fr`
- `values-it`
- `values-ja`
- `values-ko`
- `values-pt-rBR`
- `values-ru`
- `values-uk`
- `values-zh`

Findings:

- All translated resource folders are missing the same 22 `media_*` strings that
  exist in `core/resources/src/main/res/values/strings.xml`.
- Four of those are newly added toolbar content descriptions:
  `media_cd_clear_selection`, `media_cd_delete_selected`,
  `media_cd_delete_user_media`, and `media_cd_delete_room_media`.
- `media_user_created_filter_hint` was shortened in the English source; because
  none of the locale files contain that key, there is no stale translated copy,
  only a missing one.
- Several changed media files still expose hardcoded user-facing text through
  detail labels and snackbar/action messages. Those need resource extraction
  before they can be translated.

## Missing Resource-Backed Media Strings

These 22 keys are missing from every translated `core/resources` locale listed
above.

| Key | English source |
| --- | --- |
| `media_select` | Select |
| `media_delete_selected` | Delete selected |
| `media_global_cleanup` | Global cleanup (last access) |
| `media_delete_user_scoped` | Delete user media |
| `media_delete_room_scoped` | Delete room media |
| `media_user_created_filter_hint` | Optional: filter user list by upload time (Unix ms, created_ts). |
| `media_apply_date_filter` | Apply time filter |
| `media_clear_date_filter` | Clear time filter |
| `media_from_ts` | Created on/after (ms) |
| `media_until_ts` | Created on/before (ms) |
| `media_delete_user_confirm_title` | Delete this user’s media? |
| `media_delete_user_confirm_body` | Deletes local media uploaded by this user, optionally limited by the created-time filter you set. Does not remove media only referenced from other homeservers’ repos. |
| `media_delete_room_confirm_title` | Delete listed room media? |
| `media_delete_room_confirm_body` | Deletes each media item listed for this room (local and cached remote copies on this server). Room listings only include media from unencrypted events. |
| `media_section_scope` | Scope |
| `media_section_time_filter` | Upload time (optional) |
| `media_section_list` | Media |
| `media_actions_hint` | Tap a row to select or unselect. Long-press to open details. |
| `media_cd_clear_selection` | Clear selection |
| `media_cd_delete_selected` | Delete selected media |
| `media_cd_delete_user_media` | Delete all media for this user |
| `media_cd_delete_room_media` | Delete all listed media for this room |

## Suggested Locale Inserts

### `values-de/strings.xml`

```xml
<string name="media_select">Auswählen</string>
<string name="media_delete_selected">Ausgewählte löschen</string>
<string name="media_global_cleanup">Globale Bereinigung (letzter Zugriff)</string>
<string name="media_delete_user_scoped">Nutzermedien löschen</string>
<string name="media_delete_room_scoped">Raummedien löschen</string>
<string name="media_user_created_filter_hint">Optional: Nutzerliste nach Upload-Zeit filtern (Unix-ms, created_ts).</string>
<string name="media_apply_date_filter">Zeitfilter anwenden</string>
<string name="media_clear_date_filter">Zeitfilter löschen</string>
<string name="media_from_ts">Erstellt am/nach (ms)</string>
<string name="media_until_ts">Erstellt am/vor (ms)</string>
<string name="media_delete_user_confirm_title">Medien dieses Nutzers löschen?</string>
<string name="media_delete_user_confirm_body">Löscht lokale Medien, die von diesem Nutzer hochgeladen wurden, optional begrenzt durch den festgelegten Erstellungszeitfilter. Entfernt keine Medien, die nur aus Repositorys anderer Homeserver referenziert werden.</string>
<string name="media_delete_room_confirm_title">Aufgelistete Raummedien löschen?</string>
<string name="media_delete_room_confirm_body">Löscht jedes für diesen Raum aufgelistete Medienelement (lokale und zwischengespeicherte Remote-Kopien auf diesem Server). Raumlisten enthalten nur Medien aus unverschlüsselten Ereignissen.</string>
<string name="media_section_scope">Bereich</string>
<string name="media_section_time_filter">Upload-Zeit (optional)</string>
<string name="media_section_list">Medien</string>
<string name="media_actions_hint">Tippe auf eine Zeile, um sie aus- oder abzuwählen. Lange drücken, um Details zu öffnen.</string>
<string name="media_cd_clear_selection">Auswahl aufheben</string>
<string name="media_cd_delete_selected">Ausgewählte Medien löschen</string>
<string name="media_cd_delete_user_media">Alle Medien dieses Nutzers löschen</string>
<string name="media_cd_delete_room_media">Alle aufgelisteten Medien dieses Raums löschen</string>
```

### `values-es/strings.xml`

```xml
<string name="media_select">Seleccionar</string>
<string name="media_delete_selected">Eliminar seleccionados</string>
<string name="media_global_cleanup">Limpieza global (último acceso)</string>
<string name="media_delete_user_scoped">Eliminar medios del usuario</string>
<string name="media_delete_room_scoped">Eliminar medios de la sala</string>
<string name="media_user_created_filter_hint">Opcional: filtra la lista de usuarios por hora de subida (ms Unix, created_ts).</string>
<string name="media_apply_date_filter">Aplicar filtro de tiempo</string>
<string name="media_clear_date_filter">Borrar filtro de tiempo</string>
<string name="media_from_ts">Creado en/después de (ms)</string>
<string name="media_until_ts">Creado en/antes de (ms)</string>
<string name="media_delete_user_confirm_title">¿Eliminar los medios de este usuario?</string>
<string name="media_delete_user_confirm_body">Elimina los medios locales subidos por este usuario, opcionalmente limitados por el filtro de hora de creación configurado. No elimina medios que solo están referenciados desde repositorios de otros homeservers.</string>
<string name="media_delete_room_confirm_title">¿Eliminar los medios listados de la sala?</string>
<string name="media_delete_room_confirm_body">Elimina cada elemento multimedia listado para esta sala (copias locales y remotas en caché en este servidor). Las listas de salas solo incluyen medios de eventos no cifrados.</string>
<string name="media_section_scope">Ámbito</string>
<string name="media_section_time_filter">Hora de subida (opcional)</string>
<string name="media_section_list">Medios</string>
<string name="media_actions_hint">Toca una fila para seleccionarla o deseleccionarla. Mantén pulsado para abrir los detalles.</string>
<string name="media_cd_clear_selection">Borrar selección</string>
<string name="media_cd_delete_selected">Eliminar medios seleccionados</string>
<string name="media_cd_delete_user_media">Eliminar todos los medios de este usuario</string>
<string name="media_cd_delete_room_media">Eliminar todos los medios listados de esta sala</string>
```

### `values-fr/strings.xml`

```xml
<string name="media_select">Sélectionner</string>
<string name="media_delete_selected">Supprimer la sélection</string>
<string name="media_global_cleanup">Nettoyage global (dernier accès)</string>
<string name="media_delete_user_scoped">Supprimer les médias de l’utilisateur</string>
<string name="media_delete_room_scoped">Supprimer les médias de la salle</string>
<string name="media_user_created_filter_hint">Facultatif : filtrer la liste des utilisateurs par heure de téléversement (ms Unix, created_ts).</string>
<string name="media_apply_date_filter">Appliquer le filtre temporel</string>
<string name="media_clear_date_filter">Effacer le filtre temporel</string>
<string name="media_from_ts">Créé le/après (ms)</string>
<string name="media_until_ts">Créé le/avant (ms)</string>
<string name="media_delete_user_confirm_title">Supprimer les médias de cet utilisateur ?</string>
<string name="media_delete_user_confirm_body">Supprime les médias locaux téléversés par cet utilisateur, éventuellement limités par le filtre d’heure de création défini. Ne supprime pas les médias uniquement référencés depuis les dépôts d’autres homeservers.</string>
<string name="media_delete_room_confirm_title">Supprimer les médias listés de la salle ?</string>
<string name="media_delete_room_confirm_body">Supprime chaque élément multimédia listé pour cette salle (copies locales et copies distantes mises en cache sur ce serveur). Les listes de salle incluent uniquement les médias issus d’événements non chiffrés.</string>
<string name="media_section_scope">Portée</string>
<string name="media_section_time_filter">Heure de téléversement (facultatif)</string>
<string name="media_section_list">Médias</string>
<string name="media_actions_hint">Touchez une ligne pour la sélectionner ou la désélectionner. Appuyez longuement pour ouvrir les détails.</string>
<string name="media_cd_clear_selection">Effacer la sélection</string>
<string name="media_cd_delete_selected">Supprimer les médias sélectionnés</string>
<string name="media_cd_delete_user_media">Supprimer tous les médias de cet utilisateur</string>
<string name="media_cd_delete_room_media">Supprimer tous les médias listés de cette salle</string>
```

### `values-it/strings.xml`

```xml
<string name="media_select">Seleziona</string>
<string name="media_delete_selected">Elimina selezionati</string>
<string name="media_global_cleanup">Pulizia globale (ultimo accesso)</string>
<string name="media_delete_user_scoped">Elimina media dell’utente</string>
<string name="media_delete_room_scoped">Elimina media della stanza</string>
<string name="media_user_created_filter_hint">Opzionale: filtra l’elenco utenti per ora di caricamento (ms Unix, created_ts).</string>
<string name="media_apply_date_filter">Applica filtro temporale</string>
<string name="media_clear_date_filter">Cancella filtro temporale</string>
<string name="media_from_ts">Creato il/dopo (ms)</string>
<string name="media_until_ts">Creato il/prima (ms)</string>
<string name="media_delete_user_confirm_title">Eliminare i media di questo utente?</string>
<string name="media_delete_user_confirm_body">Elimina i media locali caricati da questo utente, eventualmente limitati dal filtro dell’ora di creazione impostato. Non rimuove media referenziati solo dai repository di altri homeserver.</string>
<string name="media_delete_room_confirm_title">Eliminare i media elencati della stanza?</string>
<string name="media_delete_room_confirm_body">Elimina ogni elemento multimediale elencato per questa stanza (copie locali e copie remote memorizzate nella cache su questo server). Gli elenchi delle stanze includono solo media da eventi non cifrati.</string>
<string name="media_section_scope">Ambito</string>
<string name="media_section_time_filter">Ora di caricamento (opzionale)</string>
<string name="media_section_list">Media</string>
<string name="media_actions_hint">Tocca una riga per selezionarla o deselezionarla. Tieni premuto per aprire i dettagli.</string>
<string name="media_cd_clear_selection">Cancella selezione</string>
<string name="media_cd_delete_selected">Elimina media selezionati</string>
<string name="media_cd_delete_user_media">Elimina tutti i media di questo utente</string>
<string name="media_cd_delete_room_media">Elimina tutti i media elencati di questa stanza</string>
```

### `values-ja/strings.xml`

```xml
<string name="media_select">選択</string>
<string name="media_delete_selected">選択項目を削除</string>
<string name="media_global_cleanup">全体クリーンアップ（最終アクセス）</string>
<string name="media_delete_user_scoped">ユーザーのメディアを削除</string>
<string name="media_delete_room_scoped">ルームのメディアを削除</string>
<string name="media_user_created_filter_hint">任意: アップロード時刻（Unixミリ秒、created_ts）でユーザー一覧を絞り込みます。</string>
<string name="media_apply_date_filter">時刻フィルターを適用</string>
<string name="media_clear_date_filter">時刻フィルターをクリア</string>
<string name="media_from_ts">作成日時（以降、ms）</string>
<string name="media_until_ts">作成日時（以前、ms）</string>
<string name="media_delete_user_confirm_title">このユーザーのメディアを削除しますか？</string>
<string name="media_delete_user_confirm_body">このユーザーがアップロードしたローカルメディアを削除します。設定した作成時刻フィルターで範囲を制限できます。他のホームサーバーのリポジトリから参照されているだけのメディアは削除されません。</string>
<string name="media_delete_room_confirm_title">一覧表示されたルームのメディアを削除しますか？</string>
<string name="media_delete_room_confirm_body">このルームに一覧表示された各メディア項目（このサーバー上のローカルコピーとキャッシュ済みリモートコピー）を削除します。ルーム一覧には暗号化されていないイベントのメディアのみが含まれます。</string>
<string name="media_section_scope">対象</string>
<string name="media_section_time_filter">アップロード時刻（任意）</string>
<string name="media_section_list">メディア</string>
<string name="media_actions_hint">行をタップして選択または選択解除します。長押しで詳細を開きます。</string>
<string name="media_cd_clear_selection">選択をクリア</string>
<string name="media_cd_delete_selected">選択したメディアを削除</string>
<string name="media_cd_delete_user_media">このユーザーのすべてのメディアを削除</string>
<string name="media_cd_delete_room_media">このルームの一覧表示されたすべてのメディアを削除</string>
```

### `values-ko/strings.xml`

```xml
<string name="media_select">선택</string>
<string name="media_delete_selected">선택 항목 삭제</string>
<string name="media_global_cleanup">전체 정리(마지막 접근)</string>
<string name="media_delete_user_scoped">사용자 미디어 삭제</string>
<string name="media_delete_room_scoped">방 미디어 삭제</string>
<string name="media_user_created_filter_hint">선택 사항: 업로드 시간(Unix ms, created_ts)으로 사용자 목록을 필터링합니다.</string>
<string name="media_apply_date_filter">시간 필터 적용</string>
<string name="media_clear_date_filter">시간 필터 지우기</string>
<string name="media_from_ts">생성 시각 이후(ms)</string>
<string name="media_until_ts">생성 시각 이전(ms)</string>
<string name="media_delete_user_confirm_title">이 사용자의 미디어를 삭제할까요?</string>
<string name="media_delete_user_confirm_body">이 사용자가 업로드한 로컬 미디어를 삭제하며, 설정한 생성 시간 필터로 범위를 제한할 수 있습니다. 다른 홈서버의 저장소에서 참조만 되는 미디어는 제거하지 않습니다.</string>
<string name="media_delete_room_confirm_title">목록에 표시된 방 미디어를 삭제할까요?</string>
<string name="media_delete_room_confirm_body">이 방에 표시된 각 미디어 항목(이 서버의 로컬 및 캐시된 원격 사본)을 삭제합니다. 방 목록에는 암호화되지 않은 이벤트의 미디어만 포함됩니다.</string>
<string name="media_section_scope">범위</string>
<string name="media_section_time_filter">업로드 시간(선택 사항)</string>
<string name="media_section_list">미디어</string>
<string name="media_actions_hint">행을 탭하여 선택하거나 선택 해제합니다. 길게 눌러 세부 정보를 엽니다.</string>
<string name="media_cd_clear_selection">선택 지우기</string>
<string name="media_cd_delete_selected">선택한 미디어 삭제</string>
<string name="media_cd_delete_user_media">이 사용자의 모든 미디어 삭제</string>
<string name="media_cd_delete_room_media">이 방의 목록에 표시된 모든 미디어 삭제</string>
```

### `values-pt-rBR/strings.xml`

```xml
<string name="media_select">Selecionar</string>
<string name="media_delete_selected">Excluir selecionados</string>
<string name="media_global_cleanup">Limpeza global (último acesso)</string>
<string name="media_delete_user_scoped">Excluir mídia do usuário</string>
<string name="media_delete_room_scoped">Excluir mídia da sala</string>
<string name="media_user_created_filter_hint">Opcional: filtre a lista de usuários por horário de upload (ms Unix, created_ts).</string>
<string name="media_apply_date_filter">Aplicar filtro de tempo</string>
<string name="media_clear_date_filter">Limpar filtro de tempo</string>
<string name="media_from_ts">Criado em/após (ms)</string>
<string name="media_until_ts">Criado em/antes de (ms)</string>
<string name="media_delete_user_confirm_title">Excluir a mídia deste usuário?</string>
<string name="media_delete_user_confirm_body">Exclui a mídia local enviada por este usuário, opcionalmente limitada pelo filtro de horário de criação definido. Não remove mídia apenas referenciada a partir dos repositórios de outros homeservers.</string>
<string name="media_delete_room_confirm_title">Excluir a mídia listada da sala?</string>
<string name="media_delete_room_confirm_body">Exclui cada item de mídia listado para esta sala (cópias locais e remotas em cache neste servidor). As listagens de salas incluem apenas mídia de eventos não criptografados.</string>
<string name="media_section_scope">Escopo</string>
<string name="media_section_time_filter">Horário de upload (opcional)</string>
<string name="media_section_list">Mídia</string>
<string name="media_actions_hint">Toque em uma linha para selecionar ou desmarcar. Toque e segure para abrir os detalhes.</string>
<string name="media_cd_clear_selection">Limpar seleção</string>
<string name="media_cd_delete_selected">Excluir mídia selecionada</string>
<string name="media_cd_delete_user_media">Excluir toda a mídia deste usuário</string>
<string name="media_cd_delete_room_media">Excluir toda a mídia listada desta sala</string>
```

### `values-ru/strings.xml`

```xml
<string name="media_select">Выбрать</string>
<string name="media_delete_selected">Удалить выбранное</string>
<string name="media_global_cleanup">Глобальная очистка (последний доступ)</string>
<string name="media_delete_user_scoped">Удалить медиа пользователя</string>
<string name="media_delete_room_scoped">Удалить медиа комнаты</string>
<string name="media_user_created_filter_hint">Необязательно: фильтровать список пользователей по времени загрузки (Unix мс, created_ts).</string>
<string name="media_apply_date_filter">Применить фильтр времени</string>
<string name="media_clear_date_filter">Очистить фильтр времени</string>
<string name="media_from_ts">Создано не ранее (мс)</string>
<string name="media_until_ts">Создано не позднее (мс)</string>
<string name="media_delete_user_confirm_title">Удалить медиа этого пользователя?</string>
<string name="media_delete_user_confirm_body">Удаляет локальные медиа, загруженные этим пользователем, при необходимости ограниченные заданным фильтром времени создания. Не удаляет медиа, на которые есть только ссылки из репозиториев других homeserver.</string>
<string name="media_delete_room_confirm_title">Удалить перечисленные медиа комнаты?</string>
<string name="media_delete_room_confirm_body">Удаляет каждый медиафайл, перечисленный для этой комнаты (локальные и кэшированные удалённые копии на этом сервере). Списки комнат включают только медиа из незашифрованных событий.</string>
<string name="media_section_scope">Область</string>
<string name="media_section_time_filter">Время загрузки (необязательно)</string>
<string name="media_section_list">Медиа</string>
<string name="media_actions_hint">Нажмите строку, чтобы выбрать или снять выбор. Удерживайте, чтобы открыть сведения.</string>
<string name="media_cd_clear_selection">Очистить выбор</string>
<string name="media_cd_delete_selected">Удалить выбранные медиа</string>
<string name="media_cd_delete_user_media">Удалить все медиа этого пользователя</string>
<string name="media_cd_delete_room_media">Удалить все перечисленные медиа этой комнаты</string>
```

### `values-uk/strings.xml`

```xml
<string name="media_select">Вибрати</string>
<string name="media_delete_selected">Видалити вибране</string>
<string name="media_global_cleanup">Глобальне очищення (останній доступ)</string>
<string name="media_delete_user_scoped">Видалити медіа користувача</string>
<string name="media_delete_room_scoped">Видалити медіа кімнати</string>
<string name="media_user_created_filter_hint">Необов’язково: фільтрувати список користувачів за часом завантаження (Unix мс, created_ts).</string>
<string name="media_apply_date_filter">Застосувати часовий фільтр</string>
<string name="media_clear_date_filter">Очистити часовий фільтр</string>
<string name="media_from_ts">Створено не раніше (мс)</string>
<string name="media_until_ts">Створено не пізніше (мс)</string>
<string name="media_delete_user_confirm_title">Видалити медіа цього користувача?</string>
<string name="media_delete_user_confirm_body">Видаляє локальні медіа, завантажені цим користувачем, за потреби обмежені заданим фільтром часу створення. Не видаляє медіа, на які є лише посилання з репозиторіїв інших homeserver.</string>
<string name="media_delete_room_confirm_title">Видалити перелічені медіа кімнати?</string>
<string name="media_delete_room_confirm_body">Видаляє кожен медіаелемент, перелічений для цієї кімнати (локальні та кешовані віддалені копії на цьому сервері). Списки кімнат містять лише медіа з незашифрованих подій.</string>
<string name="media_section_scope">Область</string>
<string name="media_section_time_filter">Час завантаження (необов’язково)</string>
<string name="media_section_list">Медіа</string>
<string name="media_actions_hint">Торкніться рядка, щоб вибрати або скасувати вибір. Утримуйте, щоб відкрити деталі.</string>
<string name="media_cd_clear_selection">Очистити вибір</string>
<string name="media_cd_delete_selected">Видалити вибрані медіа</string>
<string name="media_cd_delete_user_media">Видалити всі медіа цього користувача</string>
<string name="media_cd_delete_room_media">Видалити всі перелічені медіа цієї кімнати</string>
```

### `values-zh/strings.xml`

```xml
<string name="media_select">选择</string>
<string name="media_delete_selected">删除所选项</string>
<string name="media_global_cleanup">全局清理（最后访问）</string>
<string name="media_delete_user_scoped">删除用户媒体</string>
<string name="media_delete_room_scoped">删除房间媒体</string>
<string name="media_user_created_filter_hint">可选：按上传时间（Unix 毫秒，created_ts）筛选用户列表。</string>
<string name="media_apply_date_filter">应用时间筛选器</string>
<string name="media_clear_date_filter">清除时间筛选器</string>
<string name="media_from_ts">创建于/晚于（毫秒）</string>
<string name="media_until_ts">创建于/早于（毫秒）</string>
<string name="media_delete_user_confirm_title">删除此用户的媒体？</string>
<string name="media_delete_user_confirm_body">删除此用户上传的本地媒体，可按已设置的创建时间筛选器限制范围。不会删除仅由其他 homeserver 仓库引用的媒体。</string>
<string name="media_delete_room_confirm_title">删除列出的房间媒体？</string>
<string name="media_delete_room_confirm_body">删除此房间列出的每个媒体项（此服务器上的本地副本和缓存的远程副本）。房间列表仅包含未加密事件中的媒体。</string>
<string name="media_section_scope">范围</string>
<string name="media_section_time_filter">上传时间（可选）</string>
<string name="media_section_list">媒体</string>
<string name="media_actions_hint">点按一行以选择或取消选择。长按可打开详情。</string>
<string name="media_cd_clear_selection">清除选择</string>
<string name="media_cd_delete_selected">删除所选媒体</string>
<string name="media_cd_delete_user_media">删除此用户的所有媒体</string>
<string name="media_cd_delete_room_media">删除此房间列出的所有媒体</string>
```

## Hardcoded User-Facing Strings Still Needing Resource Extraction

These strings are not currently in `strings.xml`, so locale folders cannot
translate them yet.

| Source | Current hardcoded text | Suggested resource key |
| --- | --- | --- |
| `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaDetailScreen.kt` | Media ID | `media_detail_label_media_id` |
| same | Type | `media_detail_label_type` |
| same | Size | `media_detail_label_size` |
| same | Upload Name | `media_detail_label_upload_name` |
| same | Created | `media_detail_label_created` |
| same | Last Accessed | `media_detail_label_last_accessed` |
| same | Quarantined By | `media_detail_label_quarantined_by` |
| same | Protected | `media_detail_label_protected` |
| same | unknown | `unknown` or `media_detail_unknown_type` |
| same | No | `no` or `common_no` |
| same | Yes | `yes` or `common_yes` |
| `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaDetailViewModel.kt` | Media quarantined | `media_action_quarantined` |
| same | Media removed from quarantine | `media_action_unquarantined` |
| same | Media protected from quarantine | `media_action_protected` |
| same | Media protection removed | `media_action_unprotected` |
| same | Media deleted | `media_action_deleted` |
| `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaListViewModel.kt` | Invalid date range | `media_error_invalid_date_range` |
| same | Deleted %1$d items | `media_action_deleted_items` |
| same | Deleted %1$d items (%2$d failed) | `media_action_deleted_items_failed` |
| same | Deleted %1$d user media items | `media_action_deleted_user_items` |
| same | Deleted %1$d room media items | `media_action_deleted_room_items` |
| same | Deleted %1$d room media items (%2$d failed) | `media_action_deleted_room_items_failed` |

Recommended implementation note: the ViewModels currently store already-rendered
English messages in state. To localize them cleanly, store message IDs plus
format arguments, or emit typed UI events that the composable resolves with
`stringResource` before showing the snackbar.
