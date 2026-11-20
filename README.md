# DMZ: Better Visual Forms

Addon do **DragonMine Z 2.2** (Forge 1.20.1) que junta os antigos **DMZ SSJ4 Armors** e **DMZ Form Options** e acrescenta atalhos e opções visuais.
Precisa do addon no cliente (e no servidor, se jogar em servidor, para as opções dos JSON das formas sincronizarem).

> Não foi compilado nem testado dentro do jogo. Se algo não funcionar, veja `logs/latest.log` e procure por `dmz_better_visual_forms`.

## Como compilar pelo GitHub

Suba esta pasta para um repositório novo. A aba **Actions** compila sozinha; o `.jar` fica em **Artifacts → dmz-better-visual-forms**.
Se for migrar, **remova** os jars antigos `dmz_ssj4_armors` e `dmz_form_options` da pasta `mods`.

## Atalhos (mudam em Controles → DMZ: Better Visual Forms)

| Tecla | O que faz |
|---|---|
| **F6** | Liga/desliga **Minha Aura - Estilo 3D** |
| **F7** | Liga/desliga **Contornos de Transformação** |
| **F8** | **Solta/amarra a cauda** (mesmo caminho do menu radial do DMZ). Não faz nada enquanto a forma atual força a cauda (`forceLooseTail` / `forceTiedTail`) |

F9 está livre.

## Menu Visuais do DMZ (Configurações)

- **Estado de Aura ao Transformar**: *Desativado* (usa o do próprio jogo), *Temporário* (aura por ~10 s depois de transformar; `auraStateSeconds` no toml) ou *Sempre Ativo* (Estado de Aura ligado enquanto você está transformado). É só visual, no seu cliente.
- **Raios das Transformações**: *Desativado* (sem raios de forma), *Reduzido* (raios só ~5 s depois de transformar, ao usar a aura de ki, ou com o Estado de Aura ligado; `raysSeconds` no toml) ou *Sempre Ativo* (igual ao DMZ). Os raios do surto/reserva de ki não mudam.

Também ficam em `config/dmz_better_visual_forms-client.toml`.

## Roupas por forma (antigo SSJ4 Armors)

A roupa troca quando você está em **SSJ4 do GT** (pasta `ssj4gt`) ou **SSJ4 do Daima** (pasta `ssj4d`), detectados sozinhos pelo modelo da forma.
A transição **acompanha a transformação**: na lenta (segurando G) a roupa muda junto com a carga; na instantânea, o DMZ faz uma transição curta e ela segue.
A cauda também: se a forma de destino força a cauda, ela solta/amarra quando a transformação passa da metade.

Texturas (no addon ou em qualquer resource pack, namespace `dmz_ssj4_armors`, o mesmo de antes):

```
assets/dmz_ssj4_armors/textures/armor/ssj4gt/<nome da textura>.png
assets/dmz_ssj4_armors/textures/armor/ssj4d/<nome da textura>.png
```

Só precisa incluir as peças que mudam; peça que não existe na pasta usa a roupa original do DMZ. F3 + T recarrega.

Outras formas: `config/dmz_better_visual_forms-forms.json` (o arquivo do antigo `dmz_ssj4_armors-forms.json` é copiado na primeira vez):

```json
{
  "race.dragonminez.saiyan.form.supersaiyan.supersaiyan3": "ssj3",
  "race.dragonminez.saiyan.form.supersaiyan.supersaiyanmastered": "ssjdominado"
}
```

`true` = automático (pasta `ssj4d` ou `ssj4gt` pelo modelo; formas sem modelo SSJ4 ficam como estão), `false` = não troca, `"nome"` = usa a pasta `armor/nome/`. O arquivo é relido quando salva.

## Opções

| Opção | Tipo / padrão | O que faz |
|---|---|---|
| `forceTiedTail` | boolean / `false` | A cauda fica amarrada na cintura nessa forma (útil no SSJ3, em que a cauda some dentro do cabelo). |
| `forceLooseTail` | boolean / `false` | A cauda fica solta nessa forma (útil no SSJ4). Se as duas estiverem `true`, a solta vence. |
| `stackTintAffectsHairAndTail` | boolean / `true` | Vale na forma **base** (ex.: Blue). Com `false`, a tinta de uma forma empilhável (Kaio-ken) **não** pinta cabelo e cauda. |
| `stackTintHairAndTailIntensity` | número / `-1` | Vale na forma **base**. De 0 a 1: intensidade da tinta da forma empilhável **no cabelo e na cauda**. `-1` = usa a intensidade da própria forma empilhável. |
| `scleraColor` | texto / `""` | Cor da esclera (parte branca do olho), por exemplo `"#FFFFFF"`. Vazio = cor padrão do jogo. |
| `noEyebrows` | boolean / `false` | Esconde as sobrancelhas: não desenha a camada de sobrancelha e usa a textura `ssj3eyebrows_eye_N` do DMZ (a do SSJ3) por cima, escolhida pelo tipo de olho do jogador. |
| `eyeliner` | boolean / `false` | Desenha o delineado nos olhos (usa a textura `ssj4_eyes_N` do DMZ, escolhida pelo tipo de olho do jogador). |
| `eyelinerColor` | texto / `"#000000"` | Cor do delineado. |
| `chargeKiAnimation` | boolean / `true` | Com `false`, o jogador **não** faz a pose de carregar ki: fica parado (idle) enquanto a aura carrega. |

## Exemplo

```json
"supersaiyan3": {
  "forceTiedTail": true,
  "noEyebrows": true
},
"supersaiyan4": {
  "forceLooseTail": true,
  "eyeliner": true,
  "eyelinerColor": "#B00000"
},
"supersaiyanblue": {
  "stackTintAffectsHairAndTail": true,
  "stackTintHairAndTailIntensity": 0.1
},
"masteredultrainstinct": {
  "chargeKiAnimation": false,
  "scleraColor": "#FFFFFF"
}
```

(O nome da forma é a chave do objeto dentro de `"forms"`; coloque as opções junto das outras, por exemplo perto de `"hideTail"`.)

Depois de salvar, use `/dmzreload config`.

## Observações

- **Ordem no arquivo:** se o DMZ regravar o arquivo (atualização de versão), as opções novas vão para o fim de cada forma, não ao lado do `hideTail`.
- **Olhos:** esclera, sobrancelha e delineado valem para o rosto de estilo humano/saiyajin. Raças com rosto próprio (Namekuseijin, Freeza etc.) não usam essas opções.
- **Delineado:** só aparece se existir `ssj4_eyes_N.png` para o tipo de olho do jogador (o DMZ já traz as do SSJ4).
- **Tinta do Kaio-ken:** a opção vai na forma **base**. Afeta o cabelo, a cauda desenhada na camada do corpo e as partes de raça (cauda, antenas). As sobrancelhas (cor do cabelo no rosto) também usam a tinta do cabelo/cauda. Os olhos continuam com a tinta normal.
- **Mixins:** o addon altera classes do DMZ 2.2. Se uma opção não fizer nada, veja o `logs/latest.log` e procure por `dmz_form_options` (o jogo abre mesmo se algum mixin falhar).
- Não foi testado dentro do jogo.

## Compatibilidade com o DMZ Face Animations (dmz-blink)

O dmz-blink troca o caminho da textura dentro de `renderColoredLayer`. Este addon desenha os olhos pelo mesmo método
(esclera, sobrancelha, delineado), então a piscada e os olhos angulares do dmz-blink devem continuar valendo com as
opções daqui. Com `noEyebrows` o addon nem desenha a camada de sobrancelha (que o dmz-blink troca por versões angulares/piscando) e ainda aplica o apagador do SSJ3 por cima. Não foi testado junto.

## Se o cabelo não mudar com a tinta

Ao carregar o jogo, procure no `logs/latest.log` a linha `hair/tail tint override is active`. Se ela aparece, o addon
está trocando a tinta do cabelo; se o cabelo continua igual, o estilo de cabelo provavelmente tem cores próprias por
mecha, que o DMZ não tinge. Se a linha não aparece, a opção não foi lida (confira a forma base e `/dmzreload config`).

## v1.1.2

- O cabelo agora recebe a tinta também quando o estilo tem **cor própria por mecha** (o DMZ ignora a tinta global nesse caso). Mechas que herdam a cor global não são tintadas duas vezes.

## v1.1.3

- Corrigido: os mixins do cabelo e das partes da raça não eram aplicados (o método era `static` e o do DMZ não é). Por isso a tinta nunca chegava no cabelo.

## v1.2.0 — Kaioken / stack forms

- **`tintColor` vazio** (`""`): a tinta usa a cor da aura (`auraColor`) da própria forma. Só vale se `tintIntensity` for maior que 0.
- **`outlineShader.primaryColor` vazio** (`""` ou `"#"`): usa o `auraColor` da stack. `secondaryColor` vazio usa a mesma cor do primário.
- **`outlineShader.addToBaseOutline`** (boolean, `false`): com `true`, o outline da stack parte do outline da forma base (se ela tiver um) e **multiplica a espessura** dele pelo `outlineThickness` da stack (`2.0` = o dobro, `1.0` = igual). Sem isso, o outline da stack substitui o da base (comportamento original do DMZ). O `outlineShader` da stack precisa ter `"enabled": true`.
- **Tinta do Kaioken x2 → x20**: cada nível é uma forma separada com seu próprio `tintIntensity`. Deixe `stackTintHairAndTailIntensity` em `-1.0` (padrão) para o cabelo e a cauda seguirem o `tintIntensity` de cada nível. Um valor fixo (ex.: `0.1`) vale para todos os níveis e esconde a diferença.

Exemplo (x20):
```json
"tintColor": "",
"tintIntensity": 0.5,
"stackTintHairAndTailIntensity": -1.0,
"outlineShader": { "enabled": true, "primaryColor": "", "secondaryColor": "", "outlineThickness": 2.0, "addToBaseOutline": true }
```
