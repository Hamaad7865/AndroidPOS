/* products.jsx — Products list, Add/Edit Product, Reports */
const { useState: upS } = React;

function ProductsList({ theme, onTheme, onGoTo }) {
  const rows = PRODUCTS.map(p => ({
    ...p,
    rack: ["A-02","B-04","A-08","C-01","B-02","A-06","B-04","C-03","D-01","D-02","A-04","C-04"][PRODUCTS.indexOf(p)] || "A-01",
    val: p.price * p.stock,
    sold: [42,18,98,12,210,33,8,17,540,712,28,140][PRODUCTS.indexOf(p)] || 12,
  }));
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="products"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0}}>
        <AppBar title="Products" subtitle="62 SKUs · 12 categories · Rs 130.59K stock value"
          right={
            <div style={{display:"flex",gap:8}}>
              <button className="btn btn-secondary btn-sm"><Icon.download size={14}/> Export</button>
              <button className="btn btn-secondary btn-sm"><Icon.barcode size={14}/> Print labels</button>
              <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
              <button className="btn btn-primary btn-sm" onClick={()=>onGoTo&&onGoTo("add-product")}><Icon.plus size={14}/> Add product</button>
            </div>}/>
        <div style={{padding:"14px 22px 0",display:"flex",gap:10,alignItems:"center"}}>
          <div className="field" style={{flex:1,maxWidth:380}}>
            <Icon.search size={16}/><input placeholder="Search by name, SKU, barcode…"/>
          </div>
          <div style={{display:"flex",gap:6,flex:1,overflowX:"auto"}} className="scroll">
            {["All","Tools","Plumbing","Fasteners","Paint"].map((c,i)=>(
              <button key={c} className={"chip"+(i===0?" active":"")}>{c}</button>
            ))}
          </div>
          <button className="btn btn-secondary btn-sm"><Icon.filter size={14}/> Filters</button>
        </div>

        <div style={{padding:"14px 22px 22px",flex:1,overflow:"hidden"}}>
          <div className="card" style={{padding:0,height:"100%",display:"flex",flexDirection:"column",overflow:"hidden"}}>
            {/* table head */}
            <div style={th}>
              <div style={{width:34}}></div>
              <div style={{width:54}}></div>
              <div style={{flex:2,minWidth:0}}>Product</div>
              <div style={{flex:1}}>Category · Rack</div>
              <div style={{width:90,textAlign:"right"}}>Cost</div>
              <div style={{width:90,textAlign:"right"}}>Price</div>
              <div style={{width:70,textAlign:"right"}}>Stock</div>
              <div style={{width:110,textAlign:"right"}}>Value</div>
              <div style={{width:120,textAlign:"right"}}>Movement</div>
              <div style={{width:34}}></div>
            </div>
            <div className="scroll reveal" style={{flex:1,overflow:"auto"}}>
              {rows.map(r => (
                <div key={r.id} style={{...tr,cursor:"pointer"}} onClick={()=>onGoTo&&onGoTo("product-detail")}>
                  <input type="checkbox" style={{accentColor:"var(--amber)"}}/>
                  <div style={{width:54,height:54,borderRadius:8,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center",border:"1px solid var(--hairline-2)"}}>
                    <ProductTile kind={r.kind} size={50}/>
                  </div>
                  <div style={{flex:2,minWidth:0}}>
                    <div style={{fontSize:13.5,fontWeight:600}}>{r.name}</div>
                    <div style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>{r.sku} · BC 893{r.id.replace("P-","")}</div>
                  </div>
                  <div style={{flex:1,display:"flex",flexDirection:"column",gap:4}}>
                    <span style={{fontSize:12,fontWeight:600}}>{r.cat}</span>
                    <span style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>Rack {r.rack}</span>
                  </div>
                  <div className="num" style={{width:90,textAlign:"right",fontSize:13}}>Rs {Math.round(r.price*0.68).toLocaleString()}</div>
                  <div className="num" style={{width:90,textAlign:"right",fontSize:14,fontWeight:700}}>Rs {r.price.toLocaleString()}</div>
                  <div style={{width:70,textAlign:"right"}}>
                    <span className={"badge "+(r.stock<=6?"badge-low":r.stock<=20?"badge-amber":"badge-paid")}>
                      <span className="num">{r.stock}</span>
                    </span>
                  </div>
                  <div className="num" style={{width:110,textAlign:"right",fontSize:13,fontWeight:600}}>Rs {r.val.toLocaleString()}</div>
                  <div style={{width:120,display:"flex",justifyContent:"flex-end"}}>
                    <Spark data={[5,8,7,10,9,12, r.sold/10]} color="var(--amber)" w={100} h={28}/>
                  </div>
                  <button style={{border:0,background:"transparent",cursor:"pointer",color:"var(--muted)"}}><Icon.more size={16}/></button>
                </div>
              ))}
            </div>
            <div style={{padding:"10px 16px",borderTop:"1px solid var(--hairline)",background:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"space-between"}}>
              <div style={{fontSize:12,color:"var(--muted)"}}>Showing <span className="num" style={{color:"var(--ink)",fontWeight:600}}>12</span> of <span className="num" style={{color:"var(--ink)",fontWeight:600}}>62</span></div>
              <div style={{display:"flex",gap:6}}>
                <button className="btn btn-secondary btn-sm">‹</button>
                <button className="btn btn-secondary btn-sm" style={{background:"var(--ink)",color:"var(--surface)"}}>1</button>
                <button className="btn btn-secondary btn-sm">2</button>
                <button className="btn btn-secondary btn-sm">3</button>
                <button className="btn btn-secondary btn-sm">›</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
const th = {display:"flex",gap:14,alignItems:"center",padding:"12px 16px",borderBottom:"1px solid var(--hairline)",fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600,background:"var(--surface)"};
const tr = {display:"flex",gap:14,alignItems:"center",padding:"10px 16px",borderBottom:"1px solid var(--hairline-2)"};

/* ============================================================
   Add / Edit Product
   ============================================================ */
function AddProduct({ theme, onTheme, onBack }) {
  const [step, setStep] = upS(1);
  const [tax, setTax] = upS("inclusive");
  const [unit, setUnit] = upS("pcs");
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="products"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Add Product" subtitle="Create a new SKU"
          right={
            <div style={{display:"flex",gap:8}}>
              <button className="btn btn-ghost btn-sm" onClick={onBack}>Cancel</button>
              <button className="btn btn-secondary btn-sm">Save draft</button>
              <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
              <button className="btn btn-primary btn-sm"><Icon.check size={14}/> Publish</button>
            </div>}/>

        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{display:"grid",gridTemplateColumns:"260px 1fr",gap:22,maxWidth:1100,margin:"0 auto"}}>
            {/* Step rail */}
            <div style={{position:"sticky",top:0}}>
              <div className="eyebrow" style={{marginBottom:10}}>Setup steps</div>
              {[
                {n:1,t:"Identity",d:"Name, SKU, barcode"},
                {n:2,t:"Classification",d:"Category, brand, model"},
                {n:3,t:"Stock & location",d:"Rack, shelf, batch"},
                {n:4,t:"Pricing & tax",d:"Cost, sale price, VAT"},
                {n:5,t:"Review",d:"Confirm & publish"},
              ].map(s=>(
                <button key={s.n} onClick={()=>setStep(s.n)} style={{
                  display:"flex",gap:12,alignItems:"flex-start",padding:"10px 12px",borderRadius:10,
                  background: step===s.n?"var(--raised)":"transparent",
                  border: step===s.n?"1px solid var(--hairline)":"1px solid transparent",
                  width:"100%",textAlign:"left",cursor:"pointer",marginBottom:4
                }}>
                  <span style={{
                    width:24,height:24,borderRadius:99,flexShrink:0,
                    background: step>s.n?"var(--emerald)":step===s.n?"var(--ink)":"var(--raised-2)",
                    color: step>=s.n?"var(--surface)":"var(--muted)",
                    fontFamily:"var(--mono)",fontWeight:700,fontSize:11,
                    display:"flex",alignItems:"center",justifyContent:"center",
                    border: step>=s.n?"none":"1px solid var(--hairline)"
                  }}>{step>s.n?<Icon.check size={12}/>:s.n}</span>
                  <div>
                    <div style={{fontSize:13,fontWeight:700}}>{s.t}</div>
                    <div style={{fontSize:11,color:"var(--muted)",lineHeight:1.3}}>{s.d}</div>
                  </div>
                </button>
              ))}
            </div>

            {/* Form */}
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              {/* Image + identity */}
              <div className="card-machined" style={{padding:20}}>
                <div className="eyebrow" style={{marginBottom:14}}>Identity</div>
                <div style={{display:"grid",gridTemplateColumns:"180px 1fr",gap:18}}>
                  <div>
                    <div className="label">Photo</div>
                    <div style={{
                      width:160,height:160,borderRadius:14,border:"2px dashed var(--hairline)",
                      display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",gap:6,
                      background:"var(--raised-2)",cursor:"pointer",color:"var(--muted)"
                    }}>
                      <ProductTile kind="drill" size={84}/>
                      <span style={{fontSize:11,fontWeight:600}}>Drop or click</span>
                    </div>
                  </div>
                  <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                    <Field label="Product name" v="Viseuse Cordless 18V" full/>
                    <Field label="Short name (POS)" v="Viseuse 18V"/>
                    <Field label="SKU" v="VSC-18V" mono/>
                    <Field label="Barcode" v="8930041800218" mono right={<Icon.barcode size={16}/>}/>
                    <Field label="Description" v="Brushless motor, 2 batteries + charger included." full/>
                  </div>
                </div>
              </div>

              <div className="card-machined" style={{padding:20}}>
                <div className="eyebrow" style={{marginBottom:14}}>Classification & location</div>
                <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:12}}>
                  <Field label="Category" v="Tools ↓"/>
                  <Field label="Brand" v="Bosch Professional"/>
                  <Field label="Model" v="GSR 18V-90 C"/>
                  <Field label="Rack" v="A-02" mono/>
                  <Field label="Shelf" v="3" mono/>
                  <Field label="Batch no." v="BCH-2624-A" mono/>
                </div>
              </div>

              <div className="card-machined" style={{padding:20}}>
                <div className="eyebrow" style={{marginBottom:14}}>Pricing & tax</div>
                <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:12}}>
                  <Field label="Purchase price" v="985.00" mono right="Rs"/>
                  <Field label="Sale price" v="1,450.00" mono right="Rs"/>
                  <Field label="Margin" v="32.0 %" mono readOnly/>
                </div>
                <div style={{marginTop:14}}>
                  <div className="label">VAT type</div>
                  <div style={{display:"flex",gap:8}}>
                    {[{id:"inclusive",l:"Tax-inclusive (15%)"},{id:"exclusive",l:"Tax-exclusive (15%)"},{id:"none",l:"Zero-rated"}].map(o=>{
                      const on = tax===o.id;
                      return (
                        <button key={o.id} onClick={()=>setTax(o.id)} style={{
                          flex:1,padding:"12px 14px",borderRadius:10,cursor:"pointer",
                          border: on?"1.5px solid var(--ink)":"1px solid var(--hairline)",
                          background: on?"var(--raised)":"var(--raised-2)",
                          textAlign:"left",
                        }}>
                          <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
                            <span style={{fontSize:13,fontWeight:700}}>{o.l}</span>
                            <span style={{width:16,height:16,borderRadius:99,border:"2px solid "+(on?"var(--ink)":"var(--hairline)"),display:"inline-flex",alignItems:"center",justifyContent:"center"}}>
                              {on && <span style={{width:8,height:8,borderRadius:99,background:"var(--ink)"}}/>}
                            </span>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="card-machined" style={{padding:20}}>
                <div className="eyebrow" style={{marginBottom:14}}>Stock</div>
                <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:12}}>
                  <Field label="Unit" v="Pieces"/>
                  <Field label="Opening stock" v="6" mono/>
                  <Field label="Low-stock threshold" v="3" mono/>
                  <Field label="Re-order qty" v="12" mono/>
                </div>
              </div>

              <div style={{display:"flex",justifyContent:"space-between",gap:10}}>
                <button className="btn btn-secondary" onClick={onBack}>Back</button>
                <div style={{display:"flex",gap:8}}>
                  <button className="btn btn-secondary">Save draft</button>
                  <button className="btn btn-primary">Publish product <Icon.arrow_r size={16}/></button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
function Field({label, v, mono, right, readOnly, full}) {
  return (
    <div style={{gridColumn: full ? "span 2": "auto"}}>
      <div className="label">{label}</div>
      <div className="field">
        <input className={mono?"num":""} value={v} readOnly={readOnly} onChange={()=>{}} style={{fontWeight: mono?600:500}}/>
        {typeof right === "string" ? <span style={{color:"var(--muted)",fontSize:12,fontFamily:"var(--mono)"}}>{right}</span> : right}
      </div>
    </div>
  );
}

/* ============================================================
   Reports hub + sample report
   ============================================================ */
function Reports({ theme, onTheme }) {
  const tiles = [
    {t:"Sales report",     d:"By date, customer, item", ic:"cart",   c:"var(--amber)"},
    {t:"Sales returns",    d:"Refunds, credits",        ic:"refresh",c:"var(--crimson)"},
    {t:"Purchase report",  d:"Stock in, suppliers",     ic:"truck",  c:"var(--ink)"},
    {t:"Due list",         d:"Outstanding balances",    ic:"wallet", c:"var(--crimson)"},
    {t:"Day book",         d:"All transactions",        ic:"receipt",c:"var(--graphite)"},
    {t:"Bill-wise profit", d:"Per invoice margin",      ic:"chart",  c:"var(--emerald)"},
    {t:"Profit & Loss",    d:"Income vs expense",       ic:"chart",  c:"var(--emerald)"},
    {t:"Cashflow",         d:"In/out by account",       ic:"wallet", c:"var(--ink)"},
    {t:"Balance sheet",    d:"Assets · liabilities",    ic:"box",    c:"var(--ink)"},
    {t:"Tax / VAT",        d:"Per period",              ic:"filter", c:"var(--graphite)"},
    {t:"Product history",  d:"Stock movement",          ic:"box",    c:"var(--ink)"},
    {t:"Staff sales",      d:"Per cashier",             ic:"people", c:"var(--graphite)"},
  ];
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="reports"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Reports" subtitle="Period · 01 May – 26 May 2026"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm"><Icon.filter size={14}/> Custom range</button>
            <button className="btn btn-secondary btn-sm"><Icon.download size={14}/> Export</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>

          {/* P&L summary banner (sample report focus) */}
          <div className="card-machined" style={{padding:20,marginBottom:18}}>
            <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",gap:20}}>
              <div>
                <div className="eyebrow">Featured · Profit & Loss · May 2026</div>
                <h2 style={{margin:"6px 0 0",fontSize:28,fontWeight:700,letterSpacing:"-0.015em"}}>Net profit <span className="num" style={{color:"var(--emerald)"}}>Rs 286,440</span></h2>
                <div style={{display:"flex",gap:18,marginTop:10,flexWrap:"wrap"}}>
                  <Stat label="Gross sales" v="Rs 1.42M"/>
                  <Stat label="Cost of goods" v="Rs 932K"/>
                  <Stat label="Operating exp." v="Rs 201K"/>
                  <Stat label="Margin" v="34.2%" color="var(--emerald)"/>
                  <Stat label="Returns" v="Rs 8.4K" color="var(--crimson)"/>
                </div>
              </div>
              <div style={{display:"flex",gap:8}}>
                <button className="btn btn-secondary btn-sm"><Icon.print size={14}/> Print</button>
                <button className="btn btn-secondary btn-sm"><Icon.share size={14}/> Share</button>
                <button className="btn btn-primary btn-sm">Open full report <Icon.arrow_r size={14}/></button>
              </div>
            </div>

            {/* mini chart row */}
            <div style={{display:"grid",gridTemplateColumns:"2fr 1fr 1fr",gap:14,marginTop:18,paddingTop:18,borderTop:"1px solid var(--hairline-2)"}}>
              <div>
                <div className="eyebrow" style={{marginBottom:6}}>Daily net</div>
                <svg viewBox="0 0 400 80" width="100%" height="80">
                  <defs><linearGradient id="rpt-fill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0" stopColor="var(--emerald)" stopOpacity=".25"/><stop offset="1" stopColor="var(--emerald)" stopOpacity="0"/>
                  </linearGradient></defs>
                  <path d={linePath([10,18,12,22,30,28,40,35,48,55,52,60,72,78], 400, 80)+" L 400,80 L 0,80 Z"} fill="url(#rpt-fill)"/>
                  <path d={linePath([10,18,12,22,30,28,40,35,48,55,52,60,72,78], 400, 80)} fill="none" stroke="var(--emerald)" strokeWidth="2"/>
                </svg>
              </div>
              <Donut value={68} color="var(--amber)" label="Sales mix · Tools"/>
              <Donut value={42} color="var(--ink)" label="Cash vs Card"/>
            </div>
          </div>

          {/* Tiles */}
          <div className="reveal" style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:14}}>
            {tiles.map((t,i)=>{
              const IcC = Icon[t.ic];
              return (
                <button key={i} className="card-machined" style={{
                  padding:18,textAlign:"left",cursor:"pointer",display:"flex",flexDirection:"column",gap:14,
                  background:"var(--raised)"
                }}>
                  <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
                    <div style={{width:42,height:42,borderRadius:12,background:t.c,color:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                      <IcC size={20}/>
                    </div>
                    <Icon.arrow_r size={16}/>
                  </div>
                  <div>
                    <div style={{fontSize:15,fontWeight:700,letterSpacing:"-0.01em"}}>{t.t}</div>
                    <div style={{fontSize:12,color:"var(--muted)",marginTop:2}}>{t.d}</div>
                  </div>
                  <div style={{display:"flex",alignItems:"center",gap:8}}>
                    <span className="num" style={{fontSize:11,color:"var(--muted)"}}>Last run · today</span>
                    <Spark data={[3,5,4,7,6,8,10]} color={t.c} w={56} h={20} area={false}/>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
function Stat({label, v, color}) {
  return (
    <div>
      <div style={{fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600}}>{label}</div>
      <div className="num" style={{fontSize:18,fontWeight:700,marginTop:2,color: color||"var(--ink)"}}>{v}</div>
    </div>
  );
}
function Donut({value, color, label}) {
  const r=28, c=2*Math.PI*r;
  return (
    <div style={{display:"flex",alignItems:"center",gap:12}}>
      <svg width="80" height="80" viewBox="0 0 80 80">
        <circle cx="40" cy="40" r={r} stroke="var(--hairline)" strokeWidth="8" fill="none"/>
        <circle cx="40" cy="40" r={r} stroke={color} strokeWidth="8" fill="none"
          strokeDasharray={`${c*value/100} ${c}`} strokeDashoffset={c/4} strokeLinecap="round"
          transform="rotate(-90 40 40)"/>
        <text x="40" y="46" textAnchor="middle" fontFamily="JetBrains Mono" fontSize="16" fontWeight="700" fill="var(--ink)">{value}%</text>
      </svg>
      <div>
        <div style={{fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600}}>Mix</div>
        <div style={{fontSize:13,fontWeight:700,marginTop:2,lineHeight:1.2}}>{label}</div>
      </div>
    </div>
  );
}

Object.assign(window, { ProductsList, AddProduct, Reports, linePath, Spark });
