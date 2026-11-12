"""Exporte les sprites LPC (Universal LPC Spritesheet Character Generator) vers assets/lpc.

Le dépôt LPC est attendu à côté du projet : ../../lpc-generator (relatif à ce fichier).
Seule l'animation de marche est exportée : 9 images x 4 directions (haut, gauche, bas, droite), cases de 64x64.
Les objets restent dans la palette de base LPC ; le jeu les recolore avec les palettes exportées.
"""
import json
import os

from PIL import Image

ICI = os.path.dirname(os.path.abspath(__file__))
LPC = os.path.normpath(os.path.join(ICI, '..', '..', 'lpc-generator'))
SORTIE = os.path.join(ICI, 'lpc')
DEFS = os.path.join(LPC, 'sheet_definitions')
SPRITES = os.path.join(LPC, 'spritesheets')
MARCHE = (576, 256)
Z_CORPS = 10

# corps du jeu : (race, femme) -> type de corps LPC, tête, oreilles
CORPS = [
    ('humain', False, 'male', 'head/heads/human/heads_human_male', None),
    ('humain', True, 'female', 'head/heads/human/heads_human_female', None),
    ('elfe', False, 'male', 'head/heads/human/heads_human_male', 'head/ears/head_ears_elven'),
    ('elfe', True, 'female', 'head/heads/human/heads_human_female', 'head/ears/head_ears_elven'),
    ('orc', False, 'male', 'head/heads/fantasy/heads_orc_male', None),
    ('orc', True, 'female', 'head/heads/fantasy/heads_orc_female', None),
    ('nain', False, 'teen', 'head/heads/human/heads_human_male', None),
    ('nain', True, 'teen', 'head/heads/human/heads_human_female', None),
    ('humain', None, 'child', 'head/heads/human/heads_human_child', None),
    ('elfe', None, 'child', 'head/heads/human/heads_human_child', 'head/ears/head_ears_elven'),
    ('orc', None, 'child', 'head/heads/fantasy/heads_orc_child', None),
    ('nain', None, 'child', 'head/heads/human/heads_human_child', None),
]
TYPES = ['male', 'female', 'teen', 'child']

CATEGORIES = [
    ('chapeaux', [
        'headwear/coverings/hoods/hat_hood_cloth', 'headwear/coverings/hoods/hat_hood_sack_cloth',
        'headwear/coverings/bandana/hat_bandana', 'headwear/coverings/headbands/hat_headband_kerchief',
        'headwear/coverings/headbands/hat_headband_thick', 'headwear/hats/magic/hat_magic_wizard',
        'headwear/hats/caps/hat_cap_leather', 'headwear/helmets/helmets/hat_helmet_nasal',
        'headwear/helmets/helmets/hat_helmet_norman', 'headwear/helmets/helmets/hat_helmet_spangenhelm',
        'headwear/helmets/helmets/hat_helmet_barbuta', 'headwear/helmets/helmets/hat_helmet_bascinet',
        'headwear/helmets/helmets/hat_helmet_kettle', 'headwear/helmets/helmets/hat_helmet_horned',
        'headwear/helmets/helmets/hat_helmet_greathelm', 'headwear/helmets/helmets/hat_helmet_mail',
        'headwear/coverings/headbands/hat_headband_hairtie']),
    ('hauts', [
        'torso/shirts/longsleeve/torso_clothes_longsleeve', 'torso/shirts/longsleeve/torso_clothes_longsleeve2',
        'torso/shirts/longsleeve/torso_clothes_longsleeve2_vneck', 'torso/shirts/longsleeve/torso_clothes_longsleeve2_buttoned',
        'torso/shirts/longsleeve/torso_clothes_longsleeves_cuffed', 'torso/shirts/longsleeve/torso_clothes_longsleeve_scoop',
        'torso/shirts/shortsleeve/torso_clothes_shortsleeve', 'torso/shirts/shortsleeve/torso_clothes_tshirt_vneck',
        'torso/shirts/shortsleeve/torso_clothes_shortsleeve_cardigan', 'torso/shirts/sleeveless/torso_clothes_sleeveless1',
        'torso/shirts/sleeveless/torso_clothes_sleeveless2_vneck', 'torso/armour/torso_armour_leather',
        'torso/armour/torso_armour_legion', 'torso/armour/torso_armour_plate', 'torso/torso_chainmail',
        'torso/shirts/sleeveless/torso_clothes_sleeveless2_buttoned', 'torso/jacket/torso_jacket_tabard',
        'torso/jacket/torso_jacket_frock', 'torso/jacket/torso_jacket_trench', 'torso/torso_bandages',
        'torso/shirts/torso_clothes_child_shirt']),
    ('bas', [
        'legs/leggings/legs_hose', 'legs/leggings/legs_leggings', 'legs/leggings/legs_leggings2', 'legs/pants/legs_cuffed',
        'legs/pants/legs_formal', 'legs/pants/legs_formal_striped', 'legs/pants/legs_pantaloons', 'legs/pants/legs_pants',
        'legs/pants/legs_pants2', 'legs/shorts/legs_shorts', 'legs/shorts/legs_shorts_short', 'legs/skirts/legs_skirts_legion',
        'legs/skirts/legs_skirts_plain', 'legs/skirts/legs_skirts_slit', 'legs/legs_armour',
        'legs/pants/legs_childpants', 'legs/skirts/legs_childskirts']),
    ('bottes', [
        'feet/boots/feet_boots_basic', 'feet/boots/feet_boots_fold', 'feet/boots/feet_boots_revised', 'feet/boots/feet_boots_rim',
        'feet/feet_armour', 'feet/feet_sandals', 'feet/feet_slippers', 'feet/shoes/feet_shoes_basic', 'feet/shoes/feet_shoes_ghillies',
        'feet/shoes/feet_shoes_revised', 'feet/shoes/feet_shoes_sara', 'feet/socks/feet_socks_high', 'feet/socks/feet_socks_ankle']),
    ('coiffures', [
        'hair/bald/hair_buzzcut', 'hair/bald/hair_balding', 'hair/short/hair_plain', 'hair/short/hair_parted',
        'hair/short/hair_messy1', 'hair/short/hair_unkempt', 'hair/short/hair_bedhead', 'hair/long/hair_long',
        'hair/long/hair_long_messy', 'hair/long/hair_loose', 'hair/xlong/hair_xlong', 'hair/long/hair_wavy',
        'hair/braids/hair_ponytail', 'hair/braids/hair_braid', 'hair/bald/hair_longhawk', 'hair/bob/hair_bob',
        'hair/bob/hair_relm_short', 'hair/braids/hair_topknot_short', 'hair/braids/hair_topknot_long',
        'hair/short/hair_parted_side_bangs', 'hair/short/hair_swoop_side', 'hair/curly/hair_jewfro']),
    ('barbes', [
        'hair/beards/beards_5oclock_shadow', 'hair/beards/beards_trimmed', 'hair/beards/beards_beard', 'hair/beards/beards_medium',
        'hair/beards/beards_winter', 'hair/mustaches/beards_mustache', 'hair/mustaches/beards_bigstache',
        'hair/mustaches/beards_chevron', 'hair/mustaches/beards_french', 'hair/mustaches/beards_handlebar',
        'hair/mustaches/beards_horseshoe', 'hair/mustaches/beards_lampshade', 'hair/mustaches/beards_walrus']),
    ('capes', ['torso/cape/cape_tattered', 'torso/cape/cape_solid']),
    ('epaules', ['arms/shoulders/shoulders_pauldrons', 'arms/shoulders/shoulders_mantal', 'arms/shoulders/shoulders_legion',
                 'arms/bauldron']),
    ('bras', ['arms/arms_gloves', 'arms/wrists/arms_bracers', 'arms/arms_armour', 'arms/wrists/wrists_cuffs']),
    ('cou', ['headwear/neck/neck_scarf', 'headwear/neck/neck_capetie', 'headwear/neck/charms/neck_amulet_spider',
             'headwear/neck/charms/neck_amulet_cross']),
]

MATERIAUX = ['body', 'cloth', 'metal', 'hair']
BASE_DEFAUT = {}
# teintes tirées par le jeu, par nom de palette LPC (ulpc)
CHOIX = {
    'peau.humain': ('body', ['light', 'amber', 'olive', 'taupe', 'bronze', 'brown', 'black']),
    'peau.elfe': ('body', ['light', 'amber', 'olive', 'lavender']),
    'peau.orc': ('body', ['green', 'dark_green', 'pale_green', 'zombie_green']),
    'peau.nain': ('body', ['light', 'amber', 'olive', 'taupe', 'bronze']),
    'cheveux': ('hair', ['ash', 'platinum', 'white', 'gray', 'blonde', 'sandy', 'strawberry', 'gold', 'ginger', 'carrot', 'redhead',
                         'red', 'light_brown', 'chestnut', 'dark_brown', 'dark_gray', 'black', 'raven']),
    'tissu': ('cloth', ['brown', 'leather', 'walnut', 'maroon', 'navy', 'bluegray', 'forest', 'slate', 'gray', 'black',
                        'charcoal']),
    'metal': ('metal', ['iron', 'steel', 'bronze', 'copper', 'brass', 'silver']),
}


def charger_palettes():
    pal = {}
    for m in MATERIAUX:
        dossier = os.path.join(LPC, 'palette_definitions', m)
        BASE_DEFAUT[m] = json.load(open(os.path.join(dossier, 'meta_%s.json' % m), encoding='utf-8'))['base']
        pal[m] = {k: [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in v]
                  for k, v in json.load(open(os.path.join(dossier, '%s_ulpc.json' % m), encoding='utf-8')).items()}
    return pal


PAL = charger_palettes()
NOMS = {m: list(PAL[m]) for m in MATERIAUX}


def definition(chemin):
    return json.load(open(os.path.join(DEFS, chemin + '.json'), encoding='utf-8'))


def nom_base(base):
    if base is None:
        return None
    if '.' in base:
        jeu, nom = base.split('.', 1)
        return nom if jeu == 'ulpc' else None
    return base


FIXES = ['black', 'charcoal', 'brown', 'gray', 'leather', 'maroon', 'darkblue', 'green']


def couleurs_variante(d, variante):
    for k in sorted(d):
        if k.startswith('layer_'):
            for t in TYPES:
                if t in d[k]:
                    im = lire(d[k][t], variante)
                    if im is not None:
                        return {c[1][:3] for c in im.getcolors(1 << 16) if c[1][3]}
    return None


def emplacements(d):
    """Liste des (matériau, palette de base) recolorables de l'objet, et les fichiers de variante à lire
    (sans palette recolorable, chaque variante sombre devient un objet à part)."""
    rec = d.get('recolors')
    if rec is None:
        variantes = d.get('variants', [])
        if 'white' in variantes:
            cs = couleurs_variante(d, 'white')
            if cs is not None and cs <= set(PAL['cloth']['white']):
                return [('cloth', 'white')], ['white']
        return [], [v for v in FIXES if v in variantes] or variantes[:1] or [None]
    if 'material' in rec:
        rec = {'color_1': rec}
    slots = []
    for k in sorted(rec):
        v = rec[k]
        m = v.get('material')
        if m not in PAL:
            continue
        b = nom_base(v.get('base', BASE_DEFAUT[m]))
        if b in PAL[m]:
            slots.append((m, b))
    return slots, [None]


def lire(chemin_couche, variante):
    f = os.path.join(SPRITES, chemin_couche, 'walk', variante + '.png') if variante else os.path.join(SPRITES, chemin_couche, 'walk.png')
    if not os.path.exists(f):
        return None
    im = Image.open(f).convert('RGBA')
    out = Image.new('RGBA', MARCHE, (0, 0, 0, 0))
    out.paste(im.crop((0, 0, min(im.width, MARCHE[0]), min(im.height, MARCHE[1]))), (0, 0))
    return out


def couches(d, type_corps, variante):
    res = []
    for k in sorted(d):
        if not k.startswith('layer_'):
            continue
        c = d[k]
        chemin = c.get(type_corps)
        if chemin is None:
            continue
        im = lire(chemin, variante)
        if im is not None:
            res.append((c['zPos'], im))
    return sorted(res, key=lambda t: t[0])


def remapper(im, src, dst):
    table = {s: d for s, d in zip(src, dst)}
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) in table:
                px[x, y] = (*table[(r, g, b)], a)
    return im


def aplatir(liste):
    out = Image.new('RGBA', MARCHE, (0, 0, 0, 0))
    for _, im in liste:
        out.alpha_composite(im)
    return out


def main():
    for d in ('corps', 'tetes', 'oreilles', 'palettes') + tuple(c for c, _ in CATEGORIES):
        os.makedirs(os.path.join(SORTIE, d), exist_ok=True)
    credits = {}

    corps_def = definition('body/body')
    credits['body/body'] = corps_def
    for i, (_, _, t, tete, oreilles) in enumerate(CORPS):
        aplatir(couches(corps_def, t, None)).save(os.path.join(SORTIE, 'corps', '%d.png' % i))
        dt = definition(tete)
        credits[tete] = dt
        im = aplatir(couches(dt, t, None))
        slots, _ = emplacements(dt)
        base = next((b for m, b in slots if m == 'body'), 'light')
        if base != 'light':
            remapper(im, PAL['body'][base], PAL['body']['light'])
        im.save(os.path.join(SORTIE, 'tetes', '%d.png' % i))
        if oreilles:
            do = definition(oreilles)
            credits[oreilles] = do
            aplatir(couches(do, t, None)).save(os.path.join(SORTIE, 'oreilles', '%d.png' % i))

    lignes = []
    for cat, objets in CATEGORIES:
        n = 0
        for chemin in objets:
            d = definition(chemin)
            credits[chemin] = d
            slots, variantes = emplacements(d)
            for variante in variantes:
                z = 0
                dispo = []
                for t in TYPES:
                    cs = couches(d, t, variante)
                    if not cs:
                        continue
                    dispo.append(t)
                    devant = [c for c in cs if c[0] >= Z_CORPS]
                    derriere = [c for c in cs if c[0] < Z_CORPS]
                    z = devant[0][0] if devant else 0
                    aplatir(devant).save(os.path.join(SORTIE, cat, '%d_%s.png' % (n, t)))
                    if derriere:
                        aplatir(derriere).save(os.path.join(SORTIE, cat, '%d_%s_derriere.png' % (n, t)))
                if not dispo:
                    raise SystemExit('%s : aucune animation de marche' % chemin)
                s = ' '.join('%s:%d' % (m, NOMS[m].index(b)) for m, b in slots) or '-'
                nom = d['name'].replace(' ', '_') + ('_' + variante if variante and not slots else '')
                lignes.append('%s %d %d %s %s %s' % (cat, n, z, nom, ','.join(dispo), s))
                n += 1
    with open(os.path.join(SORTIE, 'objets.txt'), 'w', encoding='utf-8') as f:
        f.write('# categorie numero zPos nom corps emplacements(materiau:ligne_de_base)\n' + '\n'.join(lignes) + '\n')

    for m in MATERIAUX:
        im = Image.new('RGBA', (6, len(NOMS[m])), (0, 0, 0, 0))
        for y, nom in enumerate(NOMS[m]):
            for x, c in enumerate(PAL[m][nom]):
                im.putpixel((x, y), (*c, 255))
        im.save(os.path.join(SORTIE, 'palettes', m + '.png'))
    with open(os.path.join(SORTIE, 'teintes.txt'), 'w', encoding='utf-8') as f:
        f.write('# choix materiau lignes\n')
        for k, (m, noms) in CHOIX.items():
            f.write('%s %s %s\n' % (k, m, ' '.join(str(NOMS[m].index(n)) for n in noms)))

    with open(os.path.join(SORTIE, 'CREDITS_LPC.txt'), 'w', encoding='utf-8') as f:
        f.write('Sprites issus du Universal LPC Spritesheet Character Generator\n'
                'https://github.com/LiberatedPixelCup/Universal-LPC-Spritesheet-Character-Generator\n\n')
        for chemin, d in credits.items():
            for c in d.get('credits', []):
                f.write('%s (%s)\n  auteurs : %s\n  licences : %s\n  %s\n\n' % (
                    d.get('name', chemin), c.get('file', ''), ', '.join(c.get('authors', [])),
                    ', '.join(c.get('licenses', [])), '\n  '.join(c.get('urls', []))))
    print('ok', SORTIE)


if __name__ == '__main__':
    main()
