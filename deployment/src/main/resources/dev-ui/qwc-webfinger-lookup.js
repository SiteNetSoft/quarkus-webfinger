import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/text-field';
import '@vaadin/button';

/**
 * Lists registered resolvers, previews the local server answer and runs live client lookups.
 */
export class QwcWebFingerLookup extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; }
        .row { display: flex; gap: 0.5rem; align-items: flex-end; }
        vaadin-text-field { flex: 1; }
        pre { background: var(--lumo-contrast-5pct); padding: 0.75rem; overflow: auto; }
        ul { margin: 0; }
    `;

    static properties = {
        _resolvers: { state: true },
        _resource: { state: true },
        _preview: { state: true },
        _lookup: { state: true },
    };

    constructor() {
        super();
        this._resolvers = [];
        this._resource = 'acct:alice@example.com';
        this._preview = '';
        this._lookup = '';
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.resolvers().then(r => { this._resolvers = r.result; });
    }

    render() {
        return html`
            <div>
                <strong>Registered resolvers</strong>
                ${this._resolvers.length === 0
                    ? html`<p>None. The endpoint is not registered.</p>`
                    : html`<ul>${this._resolvers.map(r => html`<li><code>${r}</code></li>`)}</ul>`}
            </div>
            <div class="row">
                <vaadin-text-field label="Resource" .value=${this._resource}
                    @value-changed=${e => { this._resource = e.detail.value; }}></vaadin-text-field>
                <vaadin-button theme="primary" @click=${this._doPreview}>Preview server answer</vaadin-button>
                <vaadin-button @click=${this._doLookup}>Live client lookup</vaadin-button>
            </div>
            ${this._preview ? html`<div><strong>Server answer</strong><pre>${this._preview}</pre></div>` : ''}
            ${this._lookup ? html`<div><strong>Client lookup</strong><pre>${this._lookup}</pre></div>` : ''}
        `;
    }

    _doPreview() {
        this.jsonRpc.preview({ resource: this._resource })
            .then(r => { this._preview = JSON.stringify(r.result, null, 2); });
    }

    _doLookup() {
        this.jsonRpc.lookup({ resource: this._resource })
            .then(r => { this._lookup = JSON.stringify(r.result, null, 2); });
    }
}
customElements.define('qwc-webfinger-lookup', QwcWebFingerLookup);
