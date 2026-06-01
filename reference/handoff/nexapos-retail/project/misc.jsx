/* misc.jsx — Sales List, Parties, Settings hub, Scanner, Plans paywall */
const { useState: umS } = React;

const SALES_HX = [
  {no:"S-00010", who:"Ravi Soobramoney",  d:"26 May 14:08", amt:2145, status:"open", pay:"cash"},
  {no:"S-00009", who:"Walk-in",            d:"26 May 13:42", amt:3240, status:"paid", pay:"cash"},
  {no:"S-00008", who:"Chemtex Co. Ltd",    d:"26 May 12:55", amt:18420,status:"due",  pay:"credit"},
  {no:"S-00007", who:"Préfontaine M.",     d:"26 May 11:34", amt:680,  status:"paid", pay:"card"},
  {no:"S-00006", who:"Walk-in",            d:"26 May 10:11", amt:295,  status:"paid", pay:"cash"},
  {no:"S-00005", who:"Hassen Joomun",      d:"26 May 09:48", amt:5260, status:"paid", pay:"juice"},
  {no:"S-00004", who:"D. Sundoo Hardware", d:"26 May 09:12", amt:9120, status:"due",  pay:"credit"},
  {no:"S-00003", who:"Walk-in",            d:"25 May 18:22", amt:140,  status:"paid", pay:"cash"},
  {no:"S-00002", who:"Walk-in",            d:"25 May 17:55", amt:1185, status:"refunded", pay:"cash"},
  {no:"S-00001", who:"V. Ramphul",         d:"25 May 17:01", amt:4485, status:"paid", pay:"card"},
];

function SalesList({ theme, onTheme, onGoTo }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Sales" subtitle="58 invoices today · Rs 142,820"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm"><Icon.filter size={14}/> Today</button>
            <button className="btn btn-secondary btn-sm"><Icon.download size={14}/> Export</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.plus size={14}/> New sale</button>
          </div>}/>

        <div style={{padding:"14px 22px 0",display:"flex",gap:10}}>
          <div className="field" style={{flex:1,maxWidth:380}}><Icon.search size={16}/><input placeholder="Search invoice no, customer, item…"/></div>
          <div style={{display:"flex",gap:6}}>
            {["All","Paid","Due","Refunded","Open"].map((s,i)=>(
              <button key={s} className={"chip"+(i===0?" active":"")}>{s}</button>
            ))}
          </div>
        </div>

        <div style={{padding:"14px 22px 22px",flex:1,overflow:"hidden"}}>
          <div className="card" style={{height:"100%",display:"flex",flexDirection:"column",overflow:"hidden"}}>
            <div style={salesTh}>
              <div style={{width:120}}>Invoice</div>
              <div style={{flex:1}}>Customer</div>
              <div style={{width:160}}>Date / Time</div>
              <div style={{width:100}}>Payment</div>
              <div style={{width:120,textAlign:"right"}}>Amount</div>
              <div style={{width:90}}>Status</div>
              <div style={{width:32}}></div>
            </div>
            <div className="scroll reveal" style={{flex:1,overflow:"auto"}}>
              {SALES_HX.map(s=>(
                <div key={s.no} style={{...salesTr,cursor:"pointer"}} onClick={()=>onGoTo&&onGoTo("invoice-detail")}>
                  <div className="num" style={{width:120,fontWeight:700,fontSize:13.5}}>{s.no}</div>
                  <div style={{flex:1,fontSize:13.5,fontWeight:600}}>{s.who}</div>
                  <div className="num" style={{width:160,fontSize:12,color:"var(--muted)"}}>{s.d}</div>
                  <div style={{width:100,display:"flex",alignItems:"center",gap:6,fontSize:12,color:"var(--graphite)"}}>
                    {s.pay==="cash"?<Icon.cash size={14}/>:s.pay==="card"?<Icon.card size={14}/>:s.pay==="juice"?<Icon.mobile size={14}/>:<Icon.wallet size={14}/>}
                    <span style={{textTransform:"capitalize",fontWeight:600}}>{s.pay}</span>
                  </div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:15,fontWeight:700}}>Rs {s.amt.toLocaleString()}</div>
                  <div style={{width:90}}>
                    <span className={"badge "+(s.status==="paid"?"badge-paid":s.status==="due"?"badge-due":s.status==="refunded"?"badge-ghost":"badge-amber")} style={{textTransform:"capitalize"}}>
                      {s.status}
                    </span>
                  </div>
                  <button style={{width:32,border:0,background:"transparent",cursor:"pointer",color:"var(--muted)"}}><Icon.chev_r size={16}/></button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
const salesTh = {display:"flex",gap:16,alignItems:"center",padding:"12px 18px",borderBottom:"1px solid var(--hairline)",fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600,background:"var(--surface)"};
const salesTr = {display:"flex",gap:16,alignItems:"center",padding:"12px 18px",borderBottom:"1px solid var(--hairline-2)"};

/* ============================================================
   Parties
   ============================================================ */
function Parties({ theme, onTheme }) {
  const [tab, setTab] = umS("customers");
  const customers = [
    {n:"Ravi Soobramoney",  ph:"+230 5712 4408", town:"Curepipe", bal:0,     buys:18},
    {n:"Chemtex Co. Ltd",    ph:"+230 466 1188",  town:"Port Louis", bal:18420, buys:54},
    {n:"D. Sundoo Hardware", ph:"+230 5990 3322", town:"Vacoas",   bal:9120,  buys:33},
    {n:"Préfontaine Marie",  ph:"+230 5247 8801", town:"Beau Bassin", bal:0,  buys:9},
    {n:"Hassen Joomun",      ph:"+230 5814 2099", town:"Quatre Bornes", bal:0, buys:14},
    {n:"V. Ramphul",         ph:"+230 5760 4421", town:"Rose Hill", bal:1340, buys:7},
  ];
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="parties"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Parties" subtitle="218 customers · 18 suppliers · Rs 42.18K due"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.plus size={14}/> Add party</button>
          </div>}/>

        <div style={{padding:"14px 22px 0",display:"flex",alignItems:"center",gap:14}}>
          <div style={{display:"flex",gap:0,background:"var(--raised)",border:"1px solid var(--hairline)",borderRadius:10,padding:4}}>
            {[{id:"customers",l:"Customers · 218"},{id:"suppliers",l:"Suppliers · 18"}].map(t=>{
              const on = tab===t.id;
              return <button key={t.id} onClick={()=>setTab(t.id)} style={{height:34,padding:"0 18px",border:0,borderRadius:8,background:on?"var(--ink)":"transparent",color:on?"var(--surface)":"var(--ink)",fontSize:13,fontWeight:700,cursor:"pointer"}}>{t.l}</button>;
            })}
          </div>
          <div className="field" style={{flex:1,maxWidth:360}}><Icon.search size={16}/><input placeholder="Search name, phone…"/></div>
        </div>

        <div style={{flex:1,overflow:"hidden",padding:"14px 22px 22px",display:"grid",gridTemplateColumns:"1fr 380px",gap:14}}>
          <div className="card" style={{padding:0,overflow:"hidden",display:"flex",flexDirection:"column"}}>
            <div style={salesTh}>
              <div style={{width:46}}></div>
              <div style={{flex:1}}>Name</div>
              <div style={{width:140}}>Phone</div>
              <div style={{width:130}}>Locality</div>
              <div style={{width:120,textAlign:"right"}}>Lifetime</div>
              <div style={{width:120,textAlign:"right"}}>Balance</div>
            </div>
            <div className="scroll" style={{flex:1,overflow:"auto"}}>
              {customers.map((c,i)=>(
                <div key={i} style={{...salesTr,background: i===0?"var(--amber-tint)":"transparent"}}>
                  <div style={{width:46}}>
                    <span style={{width:36,height:36,borderRadius:99,display:"inline-flex",alignItems:"center",justifyContent:"center",background:i%3===0?"var(--amber-soft)":"var(--raised-2)",color:i%3===0?"var(--amber-press)":"var(--graphite)",fontFamily:"var(--mono)",fontWeight:700,fontSize:12,border:"1px solid var(--hairline-2)"}}>
                      {c.n.split(" ").map(x=>x[0]).slice(0,2).join("")}
                    </span>
                  </div>
                  <div style={{flex:1,fontSize:13.5,fontWeight:600}}>{c.n}</div>
                  <div className="num" style={{width:140,fontSize:12.5,color:"var(--graphite)"}}>{c.ph}</div>
                  <div style={{width:130,fontSize:12.5,color:"var(--muted)"}}>{c.town}</div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:13,fontWeight:600}}>× {c.buys}</div>
                  <div style={{width:120,textAlign:"right"}}>
                    {c.bal>0
                      ? <span className="badge badge-due"><span className="num">Rs {c.bal.toLocaleString()}</span></span>
                      : <span className="num" style={{fontSize:13,color:"var(--muted)"}}>Rs 0</span>}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Detail panel — customer */}
          <div className="card-machined scroll" style={{padding:18,display:"flex",flexDirection:"column",gap:14,overflow:"auto"}}>
            <div style={{display:"flex",alignItems:"center",gap:12}}>
              <div style={{width:54,height:54,borderRadius:14,background:"var(--amber-soft)",color:"var(--amber-press)",display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"var(--mono)",fontWeight:800,fontSize:18,border:"1px solid var(--hairline)"}}>RS</div>
              <div style={{flex:1}}>
                <div style={{fontSize:16,fontWeight:700}}>Ravi Soobramoney</div>
                <div className="num" style={{fontSize:11.5,color:"var(--muted)"}}>+230 5712 4408 · since Apr 2024</div>
              </div>
              <button className="btn btn-ghost btn-sm"><Icon.more size={16}/></button>
            </div>

            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
              <div className="card" style={{padding:12,boxShadow:"none"}}>
                <div className="eyebrow">Lifetime</div>
                <div className="num" style={{fontSize:20,fontWeight:800,marginTop:2}}>Rs 24.6K</div>
                <div className="num" style={{fontSize:11,color:"var(--muted)"}}>18 invoices</div>
              </div>
              <div className="card" style={{padding:12,boxShadow:"none"}}>
                <div className="eyebrow">Outstanding</div>
                <div className="num" style={{fontSize:20,fontWeight:800,marginTop:2,color:"var(--emerald)"}}>Rs 0</div>
                <div className="num" style={{fontSize:11,color:"var(--muted)"}}>credit terms · 30d</div>
              </div>
            </div>

            <div>
              <div className="eyebrow" style={{marginBottom:6}}>Recent</div>
              {[
                {n:"S-00010", d:"26 May", a:2145, s:"open"},
                {n:"S-00001", d:"25 May", a:4485, s:"paid"},
                {n:"S-09921", d:"22 May", a:680,  s:"paid"},
                {n:"S-09905", d:"19 May", a:1820, s:"paid"},
              ].map((r,i)=>(
                <div key={i} style={{display:"flex",alignItems:"center",justifyContent:"space-between",padding:"8px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                  <div>
                    <div className="num" style={{fontSize:13,fontWeight:700}}>{r.n}</div>
                    <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{r.d}</div>
                  </div>
                  <div style={{textAlign:"right"}}>
                    <div className="num" style={{fontSize:13,fontWeight:700}}>Rs {r.a.toLocaleString()}</div>
                    <span className={"badge "+(r.s==="paid"?"badge-paid":"badge-amber")}>{r.s}</span>
                  </div>
                </div>
              ))}
            </div>

            <div style={{display:"flex",gap:8}}>
              <button className="btn btn-secondary" style={{flex:1}}><Icon.share size={14}/> Statement</button>
              <button className="btn btn-primary" style={{flex:1}}><Icon.cart size={14}/> New sale</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Settings hub
   ============================================================ */
function Settings({ theme, onTheme, onGoTo }) {
  const groups = [
    {t:"Business",   items:[
      {ic:"box",     l:"Business profile",     d:"Name, address, BRN, VAT", to:"business-profile"},
      {ic:"print",   l:"Printing & invoice",   d:"Layout · custom template", to:""},
      {ic:"cart",    l:"Sales settings",       d:"Discounts, rounding, tip", to:""},
      {ic:"filter",  l:"VAT & Tax rates",      d:"15% · 0% · custom", to:""},
    ]},
    {t:"People & access", items:[
      {ic:"people",  l:"Staff & roles",        d:"4 cashiers · 2 managers", to:"staff"},
      {ic:"setting", l:"Permissions",          d:"Discount caps, refund rights", to:""},
    ]},
    {t:"Money", items:[
      {ic:"wallet",  l:"Cash & bank accounts", d:"Till · MCB current · Juice", to:"money"},
      {ic:"chart",   l:"Income categories",    d:"6 categories", to:"income"},
    ]},
    {t:"App", items:[
      {ic:"setting", l:"Currency & locale",    d:"Rs MUR · en-MU", to:""},
      {ic:"barcode", l:"Hardware",             d:"Cash drawer · scanner", to:""},
      {ic:"refresh", l:"Sync & backup",        d:"Last 14:07 · auto-daily", to:""},
      {ic:"star",    l:"Subscription",         d:"Pro plan · Rs 1,490/mo", to:""},
    ]},
  ];
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="settings"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Settings" subtitle="QUINCAILLERIE RB TRADING · v 2.4"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-secondary btn-sm">Help</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{maxWidth:980,margin:"0 auto",display:"flex",flexDirection:"column",gap:18}}>

            <div className="card-machined" style={{padding:18,display:"flex",alignItems:"center",gap:16}}>
              <div style={{width:64,height:64,borderRadius:14,background:"var(--ink)",color:"var(--amber)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                <Icon.logo size={36}/>
              </div>
              <div style={{flex:1}}>
                <div style={{fontSize:18,fontWeight:800,letterSpacing:"-0.015em"}}>QUINCAILLERIE RB TRADING</div>
                <div className="num" style={{fontSize:12,color:"var(--muted)"}}>BRN C20177445 · VAT VAT20188822 · Royal Rd, Curepipe</div>
              </div>
              <button className="btn btn-secondary btn-sm">Edit</button>
            </div>

            {/* Theme switch card */}
            <div className="card-machined" style={{padding:18,display:"flex",alignItems:"center",gap:18}}>
              <div style={{flex:1}}>
                <div className="eyebrow">Appearance</div>
                <div style={{fontSize:15,fontWeight:700,marginTop:2}}>Theme</div>
                <div style={{fontSize:12,color:"var(--muted)"}}>Daylight is the default. Switch to Counter Mode for glare-free counters and night shifts.</div>
              </div>
              <div style={{display:"flex",gap:8}}>
                {[{id:"light",l:"Daylight",ic:"sun"},{id:"dark",l:"Counter Mode",ic:"moon"}].map(o=>{
                  const on = theme===o.id; const IcC = Icon[o.ic];
                  return (
                    <button key={o.id} onClick={()=>{ if (theme!==o.id) onTheme(); }} style={{
                      padding:"10px 14px", borderRadius:10, cursor:"pointer",
                      border: on?"1.5px solid var(--ink)":"1px solid var(--hairline)",
                      background: on?"var(--ink)":"var(--raised)",
                      color: on?"var(--surface)":"var(--ink)",
                      display:"inline-flex",alignItems:"center",gap:8,fontWeight:700,fontSize:13
                    }}>
                      <IcC size={14}/> {o.l}
                    </button>
                  );
                })}
              </div>
            </div>

            {groups.map(g=>(
              <div key={g.t}>
                <div className="eyebrow" style={{marginBottom:10}}>{g.t}</div>
                <div className="card" style={{padding:0,overflow:"hidden"}}>
                  {g.items.map((it,i)=>{const IcC = Icon[it.ic]; return (
                    <button key={i} onClick={()=>it.to&&onGoTo&&onGoTo(it.to)} style={{
                      display:"flex",alignItems:"center",gap:14,padding:"14px 18px",width:"100%",
                      background:"transparent",border:0,borderBottom: i<g.items.length-1?"1px solid var(--hairline-2)":"none",
                      cursor:"pointer",textAlign:"left",color:"var(--ink)"
                    }}>
                      <div style={{width:38,height:38,borderRadius:10,background:"var(--raised-2)",border:"1px solid var(--hairline)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                        <IcC size={16}/>
                      </div>
                      <div style={{flex:1}}>
                        <div style={{fontSize:14,fontWeight:700}}>{it.l}</div>
                        <div style={{fontSize:12,color:"var(--muted)"}}>{it.d}</div>
                      </div>
                      <Icon.chev_r size={16} stroke="currentColor" style={{color:"var(--muted)"}}/>
                    </button>
                  )})}
                </div>
              </div>
            ))}

            <div className="card" style={{padding:18, borderColor:"var(--crimson-soft)", background: "color-mix(in oklch, var(--crimson-soft) 30%, var(--raised))"}}>
              <div className="eyebrow" style={{color:"var(--crimson)"}}>Danger zone</div>
              <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginTop:8,gap:14}}>
                <div>
                  <div style={{fontSize:14,fontWeight:700}}>Delete business data</div>
                  <div style={{fontSize:12,color:"var(--muted)"}}>This will erase all sales, products, and reports for this shop. Cannot be undone.</div>
                </div>
                <button className="btn btn-danger btn-sm"><Icon.trash size={14}/> Delete</button>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Barcode scanner screen
   ============================================================ */
function Scanner({ theme, onTheme }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Scan barcode" subtitle="Point camera at any EAN-13 or QR"
          right={<button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>}/>
        <div style={{flex:1,padding:22,display:"grid",gridTemplateColumns:"1.4fr 1fr",gap:18}}>
          {/* Camera view */}
          <div style={{borderRadius:18,overflow:"hidden",position:"relative",background:"#0a0908"}}>
            <div style={{position:"absolute",inset:0,backgroundImage:"radial-gradient(circle at center, rgba(255,180,120,0.05), transparent 70%)"}}/>
            {/* fake counter table */}
            <svg width="100%" height="100%" viewBox="0 0 600 500" preserveAspectRatio="xMidYMid slice">
              <defs>
                <linearGradient id="cnt" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0" stopColor="#2a241e"/><stop offset="1" stopColor="#0a0908"/>
                </linearGradient>
              </defs>
              <rect width="600" height="500" fill="url(#cnt)"/>
              {/* product box on counter */}
              <g transform="translate(160 180)">
                <rect width="280" height="190" rx="6" fill="#FBF7EE"/>
                <rect x="0" y="0" width="280" height="36" fill="#E8651D"/>
                <text x="140" y="24" fontFamily="Hanken Grotesk" fontWeight="800" fontSize="14" textAnchor="middle" fill="#fff">VISEUSE 18V</text>
                {/* barcode */}
                <g transform="translate(40 70)">
                  {Array.from({length:48}).map((_,i)=>(
                    <rect key={i} x={i*4} y="0" width={(i%3===0?2.4:i%2?1.6:1)} height="60" fill="#14110C"/>
                  ))}
                </g>
                <text x="140" y="160" fontFamily="JetBrains Mono" fontSize="13" textAnchor="middle" fill="#14110C">8 930041 800218</text>
              </g>
            </svg>

            {/* Frame */}
            <div style={{
              position:"absolute",left:"50%",top:"50%",transform:"translate(-50%,-50%)",
              width:"60%",aspectRatio:"5/3",
              borderRadius:14,
            }}>
              {/* corners */}
              {[
                {top:0,left:0,bd:"2px solid var(--amber)",bl:"2px solid var(--amber)",br:"none",bt:"2px solid var(--amber)"},
              ].map(()=>null)}
              {[[0,0,'tl'],[0,'auto','tr'],['auto',0,'bl'],['auto','auto','br']].map(([t,l,k],i)=>(
                <div key={i} style={{position:"absolute",
                  top:t==='auto'?'auto':t,bottom:t==='auto'?0:'auto',
                  left:l==='auto'?'auto':l,right:l==='auto'?0:'auto',
                  width:32,height:32,
                  borderTop: k.includes('t')?'3px solid var(--amber)':'none',
                  borderBottom: k.includes('b')?'3px solid var(--amber)':'none',
                  borderLeft: k.includes('l')?'3px solid var(--amber)':'none',
                  borderRight: k.includes('r')?'3px solid var(--amber)':'none',
                  borderRadius: k.replace('t','t').replace('l','l')==='tl'?'6px 0 0 0':k==='tr'?'0 6px 0 0':k==='bl'?'0 0 0 6px':'0 0 6px 0'
                }}/>
              ))}
              {/* scanline */}
              <div style={{position:"absolute",left:8,right:8,top:"50%",height:2,background:"var(--amber)",boxShadow:"0 0 12px var(--amber)"}}/>
            </div>

            <div style={{position:"absolute",bottom:20,left:0,right:0,textAlign:"center",color:"#F4ECDD",fontFamily:"var(--mono)",fontSize:12,letterSpacing:".14em"}}>HOLD STEADY · SCANNING…</div>

            <div style={{position:"absolute",top:14,left:14,display:"flex",gap:8}}>
              <button className="btn btn-secondary btn-sm" style={{background:"rgba(0,0,0,0.4)",color:"#fff",border:"1px solid rgba(255,255,255,0.18)"}}><Icon.close size={14}/></button>
            </div>
            <div style={{position:"absolute",top:14,right:14,display:"flex",gap:8}}>
              <button className="btn btn-secondary btn-sm" style={{background:"rgba(0,0,0,0.4)",color:"#fff",border:"1px solid rgba(255,255,255,0.18)"}}><Icon.sun size={14}/> Torch</button>
              <button className="btn btn-secondary btn-sm" style={{background:"rgba(0,0,0,0.4)",color:"#fff",border:"1px solid rgba(255,255,255,0.18)"}}>Switch camera</button>
            </div>
          </div>

          {/* Right — last detected */}
          <div style={{display:"flex",flexDirection:"column",gap:14}}>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow">Detected</div>
              <div className="num" style={{fontSize:22,fontWeight:800,letterSpacing:"-0.015em",marginTop:6}}>8 930 041 800 218</div>
              <div style={{display:"flex",alignItems:"center",gap:12,marginTop:14}}>
                <div style={{width:64,height:64,borderRadius:10,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center",border:"1px solid var(--hairline-2)"}}>
                  <ProductTile kind="drill" size={58}/>
                </div>
                <div style={{flex:1}}>
                  <div style={{fontSize:15,fontWeight:700}}>Viseuse Cordless 18V</div>
                  <div className="num" style={{fontSize:12,color:"var(--muted)"}}>VSC-18V · Tools · Rack A-02</div>
                  <div className="num" style={{fontSize:18,fontWeight:800,marginTop:4}}>Rs 1,450</div>
                </div>
                <span className="badge badge-low">6 left</span>
              </div>
              <button className="btn btn-primary btn-lg" style={{width:"100%",marginTop:14}}>Add to ticket S-00010 <Icon.arrow_r size={16}/></button>
            </div>

            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow" style={{marginBottom:8}}>Recently scanned</div>
              {[
                {n:"PVC Pipe 110mm × 3m",k:"pipe",bc:"8930041800713",p:480},
                {n:"Spray Paint Matt Black",k:"paint",bc:"8930041801278",p:175},
                {n:"Bolt M10 Hex Galv",k:"generic",bc:"8930041801040",p:18},
              ].map((r,i)=>(
                <div key={i} style={{display:"flex",alignItems:"center",gap:10,padding:"8px 0",borderBottom:i<2?"1px solid var(--hairline-2)":"none"}}>
                  <div style={{width:34,height:34,borderRadius:7,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}><ProductTile kind={r.k} size={30}/></div>
                  <div style={{flex:1,minWidth:0}}>
                    <div style={{fontSize:13,fontWeight:600}}>{r.n}</div>
                    <div className="num" style={{fontSize:10.5,color:"var(--muted)"}}>{r.bc}</div>
                  </div>
                  <div className="num" style={{fontSize:13,fontWeight:700}}>Rs {r.p}</div>
                </div>
              ))}
            </div>

            <div className="card" style={{padding:16, background:"var(--amber-tint)", borderColor:"var(--amber-soft)"}}>
              <div style={{display:"flex",gap:10,alignItems:"flex-start"}}>
                <Icon.barcode size={18} stroke="var(--amber-press)"/>
                <div style={{fontSize:12.5,color:"var(--graphite)",lineHeight:1.4}}>
                  <strong>Manual entry:</strong> press the keypad to type a barcode or SKU if the camera can’t read.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { SalesList, Parties, Settings, Scanner });
