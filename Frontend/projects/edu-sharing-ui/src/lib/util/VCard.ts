/**
 * Created by Torsten on 28.02.2017.
 */

export class VCard {
    public static TYPE_PERSON = 0;
    public static TYPE_ORG = 1;
    // was the vcard altered
    private isDirty = false;
    private lines: string[];
    constructor(vcard: string = null) {
        if (vcard) {
            this.lines = vcard.split('\n');
            // this.remove("FN");
            if (!this.get('BEGIN')) {
                this.lines.splice(0, 0, 'BEGIN:VCARD');
            }
            if (!this.get('VERSION')) {
                this.lines.splice(1, 0, 'VERSION:3.0');
            }
            if (!this.get('END')) {
                this.lines.splice(this.lines.length, 0, 'END:VCARD');
            }
            this.isDirty = true;
        } else {
            this.lines = [];
            this.lines.push('BEGIN:VCARD');
            this.lines.push('VERSION:3.0');
            this.lines.push('END:VCARD');
            this.isDirty = false;
        }
    }

    /**
     * returns true if this vcard seems to have valid data
     * @param {string} data
     */
    public isValid() {
        // return this.lines!=null && this.lines.length;
        return (
            this.givenname ||
            this.surname ||
            this.org ||
            this.street ||
            this.city ||
            this.zip ||
            this.country
        );
    }
    public set givenname(data: string) {
        this.set('Givenname', data);
    }
    public set surname(data: string) {
        this.set('Surname', data);
    }
    public set title(data: string) {
        this.set('TITLE', data);
    }
    public set org(data: string) {
        this.set('ORG', data);
    }
    public set orgPhone(data: string) {
        this.set('TEL;TYPE=WORK,VOICE', data);
    }
    public set url(data: string) {
        this.set('URL', data);
    }
    public set email(data: string) {
        this.set('EMAIL;TYPE=PREF,INTERNET', data);
    }
    public set uid(data: string) {
        this.set('UID:urn:uuid', data);
    }
    public set orcid(data: string) {
        this.set('X-ORCID', data);
    }
    public set gnduri(data: string) {
        this.set('X-GND-URI', data);
    }
    public set ror(data: string) {
        this.set('X-ROR', data);
    }
    public set wikidata(data: string) {
        this.set('X-Wikidata', data);
    }
    public set street(data: string) {
        this.set('ADR;TYPE=intl,postal,parcel,work', data, 2);
    }
    public set city(data: string) {
        this.set('ADR;TYPE=intl,postal,parcel,work', data, 3);
    }
    public set region(data: string) {
        this.set('ADR;TYPE=intl,postal,parcel,work', data, 4);
    }
    public set zip(data: string) {
        this.set('ADR;TYPE=intl,postal,parcel,work', data, 5);
    }
    public set country(data: string) {
        this.set('ADR;TYPE=intl,postal,parcel,work', data, 6);
    }
    public set contributeDate(data: string) {
        this.set('X-ES-LOM-CONTRIBUTE-DATE', data);
    }
    public get givenname() {
        return this.get('Givenname');
    }
    public get surname() {
        return this.get('Surname');
    }
    public get orcid() {
        return this.get('X-ORCID');
    }
    public get gnduri() {
        return this.get('X-GND-URI');
    }
    public get ror() {
        return this.get('X-ROR');
    }
    public get wikidata() {
        return this.get('X-Wikidata');
    }
    public get title() {
        return this.get('TITLE');
    }
    public get org() {
        return this.get('ORG');
    }
    public get contributeDate() {
        const string = this.get('X-ES-LOM-CONTRIBUTE-DATE');
        if (string) return string.split('T')[0];
        return string;
    }
    public get url() {
        return this.get('URL');
    }
    public get orgPhone() {
        return this.get('TEL;TYPE=WORK,VOICE');
    }
    public get email() {
        return this.get('EMAIL;TYPE=PREF,INTERNET');
    }
    public get uid() {
        return this.get('UID:urn:uuid');
    }
    public get street() {
        return this.get('ADR;TYPE=intl,postal,parcel,work', 2);
    }
    public get city() {
        return this.get('ADR;TYPE=intl,postal,parcel,work', 3);
    }
    public get region() {
        return this.get('ADR;TYPE=intl,postal,parcel,work', 4);
    }
    public get zip() {
        return this.get('ADR;TYPE=intl,postal,parcel,work', 5);
    }
    public get country() {
        return this.get('ADR;TYPE=intl,postal,parcel,work', 6);
    }
    public toVCardString() {
        // unaltered -> so return a null value
        if (!this.isDirty) return null;
        if (this.get('Givenname') || this.get('Surname'))
            this.set('FN', this.get('Givenname', -1, '') + ' ' + this.get('Surname', -1, ''));
        return this.lines.join('\n');
    }
    public getDisplayName() {
        const string =
            (this.title ?? '') + ' ' + (this.givenname ?? '') + ' ' + (this.surname ?? '');
        if (string.trim() === '') {
            return this.org || this.get('FN') || '';
        }
        return string;
    }
    public get(key: string, splitIndex = -1, fallback: string = null) {
        if (key == 'Surname') {
            splitIndex = 0;
            key = 'N';
        }
        if (key == 'Givenname') {
            splitIndex = 1;
            key = 'N';
        }
        for (let line of this.lines) {
            line = line.trim();
            if (line.startsWith(key + ':')) {
                const value = line.substring(key.length + 1).trim();
                if (splitIndex != -1) return this.deEscape(value.split(';')[splitIndex]);
                return this.deEscape(value);
            }
        }
        return fallback;
    }
    public remove(key: string) {
        for (let i = 0; i < this.lines.length; i++) {
            if (this.lines[i].startsWith(key + ':')) {
                this.lines.splice(i, 1);
                return true;
            }
        }
        return false;
    }
    public set(key: string, value: string, splitIndex = -1) {
        // no value and empty vcard -> do nothing
        if (!this.isDirty && !value) return;
        if (key == 'Surname') {
            splitIndex = 0;
            key = 'N';
        }
        if (key == 'Givenname') {
            splitIndex = 1;
            key = 'N';
        }
        let i = 0;
        for (let line of this.lines) {
            line = line.trim();
            if (line.startsWith(key + ':')) {
                let lineValue = line.substring(key.length + 1).trim();
                if (splitIndex != -1) {
                    const split = lineValue.split(';');
                    split[splitIndex] = this.escape(value);
                    lineValue = split.join(';');
                } else {
                    lineValue = this.escape(value);
                }
                this.lines[i] = line.substring(0, key.length + 1) + lineValue;
                return;
            }
            i++;
        }
        let lineValue = this.escape(value);
        if (splitIndex >= 0) {
            lineValue = '';
            while (lineValue.length < splitIndex) lineValue += ';';
            lineValue += this.escape(value);
        }
        this.lines.splice(this.getNextLine(), 0, key + ':' + lineValue);
        this.isDirty = true;
    }

    private getNextLine() {
        let i = 0;
        for (const line of this.lines) {
            if (line.trim() == 'END:VCARD') return i - 1;
            i++;
        }
        return this.lines.length - 1;
    }
    private deEscape(value: string) {
        if (value == null) return '';
        return value.replace(/\\\\/g, '\\').replace(/\\,/g, ',');
    }
    private escape(value: string) {
        if (value == null) return '';
        return value.replace(/\\/g, '\\\\').replace(/,/g, '\\,').replace(/;/g, ' ');
    }

    public copy() {
        return new VCard(this.toVCardString());
    }

    /**
     * Apply all data from an other vcard to this object
     * @param other
     */
    public apply(other: VCard) {
        this.lines = other.lines;
    }

    getType(): number {
        return this.isValid()
            ? this.givenname || this.surname
                ? VCard.TYPE_PERSON
                : VCard.TYPE_ORG
            : VCard.TYPE_PERSON;
    }
}
