/* extras.jsx — Invoice detail, Product detail, Sales return, Low stock, Stock adjust, Staff, Business profile, Search */
const { useState: uxS } = React;

/* ============================================================
   Invoice detail
   ============================================================ */
function InvoiceDetail({ theme, onTheme, onBack }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Invoice S-00010" subtitle="26 May 2026 · 14:08 · paid in cash"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Back</button>
            <button className="btn btn-secondary btn-sm"><Icon.print size={14}/> Print</button>
            <button className="btn btn-secondary btn-sm"><Icon.share size={14}/> Share</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{display:"grid",gridTemplateColumns:"1fr 340px",gap:18,maxWidth:1200}}>
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              <div className="card-machined" style={{padding:18,display:"flex",alignItems:"center",gap:14}}>
                <div style={{width:50,height:50,borderRadius:12,background:"var(--emerald-soft)",color:"var(--emerald)",display:"flex",alignItems:"center",justifyContent:"center"}}><Icon.check size={22}/></div>
                <div style={{flex:1}}>
                  <div style={{fontSize:18,fontWeight:700}}>Paid in full</div>
                  <div style={{fontSize:12,color:"var(--muted)",fontFamily:"var(--mono)"}}>Cash · received Rs 3,000 · change Rs 855</div>
                </div>
                <span className="badge badge-paid">● Paid</span>
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow" style={{marginBottom:10}}>Line items</div>
                {[
                  {n:"T-Type Wrench 17mm",k:"wrench",q:2,p:185},
                  {n:"Hammer Claw 16oz Forged",k:"hammer",q:1,p:320},
                  {n:"Sprayer 20L Hi-Pressure",k:"sprayer",q:1,p:1250},
                ].map((l,i)=>(
                  <div key={i} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                    <div style={{width:40,height:40,borderRadius:8,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}><ProductTile kind={l.k} size={36}/></div>
                    <div style={{flex:1}}>
                      <div style={{fontSize:13,fontWeight:600}}>{l.n}</div>
                      <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{l.q} × Rs {l.p.toLocaleString()}</div>
                    </div>
                    <div className="num" style={{fontSize:14,fontWeight:700}}>Rs {(l.q*l.p).toLocaleString()}</div>
                  </div>
                ))}
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow" style={{marginBottom:10}}>Timeline</div>
                {[
                  {t:"Payment received · cash",d:"14:08",c:"var(--emerald)"},
                  {t:"Invoice generated",d:"14:08",c:"var(--amber)"},
                  {t:"Items scanned & added",d:"14:06",c:"var(--ink)"},
                  {t:"Ticket opened",d:"14:05",c:"var(--muted)"},
                ].map((ev,i)=>(
                  <div key={i} style={{display:"flex",gap:12,paddingBottom:12}}>
                    <span style={{width:8,height:8,borderRadius:99,background:ev.c,marginTop:5,flexShrink:0}}/>
                    <div>
                      <div style={{fontSize:13,fontWeight:600}}>{ev.t}</div>
                      <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{ev.d}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow">Customer</div>
                <div style={{fontSize:16,fontWeight:700,marginTop:6}}>Ravi Soobramoney</div>
                <div className="num" style={{fontSize:12,color:"var(--muted)"}}>+230 5712 4408 · Curepipe</div>
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow">Totals</div>
                <div style={{marginTop:8,display:"flex",flexDirection:"column",gap:4}}>
                  <XRow l="Subtotal" v="Rs 1,940"/>
                  <XRow l="Discount" v="Rs 0"/>
                  <XRow l="VAT 15%" v="Rs 205"/>
                  <div style={{borderTop:"1px dashed var(--hairline)",margin:"4px 0"}}/>
                  <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline"}}>
                    <span style={{fontSize:11,letterSpacing:".06em",textTransform:"uppercase",fontWeight:600,color:"var(--muted)"}}>Total</span>
                    <span className="num" style={{fontSize:24,fontWeight:800}}>Rs 2,145</span>
                  </div>
                  <XRow l="Cashier" v="S. Khan"/>
                  <XRow l="Counter" v="01"/>
                </div>
              </div>
              <div style={{display:"flex",gap:8}}>
                <button className="btn btn-secondary" style={{flex:1}} onClick={()=>onBack&&onBack("sales-return")}><Icon.refresh size={14}/> Return</button>
                <button className="btn btn-secondary" style={{flex:1}}><Icon.receipt size={14}/> Duplicate</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
function XRow({l,v}){return(<div style={{display:"flex",justifyContent:"space-between",fontSize:13}}><span style={{color:"var(--muted)"}}>{l}</span><span className="num" style={{fontWeight:600}}>{v}</span></div>);}

/* ============================================================
   Product detail
   ============================================================ */
function ProductDetail({ theme, onTheme, onBack }) {
  const mvt = [14,18,12,22,30,28,40,35,48,55,52,60,72,68];
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="products"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Sprayer 20L Hi-Pressure" subtitle="SPR-20L · Plumbing"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Products</button>
            <button className="btn btn-secondary btn-sm"><Icon.barcode size={14}/> Print label</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.setting size={14}/> Edit</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{display:"grid",gridTemplateColumns:"300px 1fr",gap:18}}>
            {/* Left — image + quick info */}
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              <div className="card-machined" style={{padding:18,display:"flex",flexDirection:"column",alignItems:"center",gap:14}}>
                <div style={{width:"100%",height:220,borderRadius:12,background:"var(--raised-2)",border:"1px solid var(--hairline-2)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  <ProductTile kind="sprayer" size={180}/>
                </div>
                <div className="num" style={{fontSize:34,fontWeight:800,letterSpacing:"-0.02em"}}>Rs 1,250</div>
                <div style={{width:"100%",display:"grid",gridTemplateColumns:"1fr 1fr",gap:8}}>
                  <div className="card" style={{padding:10,textAlign:"center",boxShadow:"none"}}>
                    <div className="eyebrow" style={{fontSize:9}}>Cost</div>
                    <div className="num" style={{fontSize:16,fontWeight:700,marginTop:2}}>Rs 850</div>
                  </div>
                  <div className="card" style={{padding:10,textAlign:"center",boxShadow:"none"}}>
                    <div className="eyebrow" style={{fontSize:9}}>Margin</div>
                    <div className="num" style={{fontSize:16,fontWeight:700,marginTop:2,color:"var(--emerald)"}}>32.0%</div>
                  </div>
                </div>
              </div>
              <div className="card-machined" style={{padding:16}}>
                <div className="eyebrow" style={{marginBottom:8}}>Stock</div>
                <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8}}>
                  <div><div style={{fontSize:11,color:"var(--muted)"}}>In stock</div><div className="num" style={{fontSize:18,fontWeight:800}}>14</div></div>
                  <div><div style={{fontSize:11,color:"var(--muted)"}}>Rack</div><div style={{fontSize:14,fontWeight:700}}>A-02</div></div>
                  <div><div style={{fontSize:11,color:"var(--muted)"}}>Low threshold</div><div className="num" style={{fontSize:14,fontWeight:700}}>5</div></div>
                  <div><div style={{fontSize:11,color:"var(--muted)"}}>Re-order qty</div><div className="num" style={{fontSize:14,fontWeight:700}}>20</div></div>
                </div>
              </div>
            </div>

            {/* Right — details + history */}
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:12}}>
                <StatMini eyebrow="Sold · 30d" v="42" delta="+18%"/>
                <StatMini eyebrow="Revenue" v="Rs 52.5K" delta="+22%"/>
                <StatMini eyebrow="Avg / day" v="1.4" delta="+8%"/>
                <StatMini eyebrow="Last sold" v="Today" delta="14:08"/>
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow" style={{marginBottom:8}}>Sales movement · 14 days</div>
                <svg viewBox="0 0 600 120" width="100%" height="120">
                  <defs><linearGradient id="pd-f" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="var(--amber)" stopOpacity=".2"/><stop offset="1" stopColor="var(--amber)" stopOpacity="0"/></linearGradient></defs>
                  <path d={linePath(mvt,600,120)+" L 600,120 L 0,120 Z"} fill="url(#pd-f)"/>
                  <path d={linePath(mvt,600,120)} fill="none" stroke="var(--amber)" strokeWidth="2.4" strokeLinejoin="round"/>
                </svg>
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow" style={{marginBottom:8}}>Details</div>
                <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:12}}>
                  {[
                    ["SKU","SPR-20L"],["Barcode","8930041800101"],["Category","Plumbing"],
                    ["Brand","Local"],["Unit","Pieces"],["VAT","Tax-inclusive 15%"],
                    ["Batch","SPR-2624-A"],["Shelf","3"],["Added","12 Apr 2024"],
                  ].map(([l,v],i)=>(
                    <div key={i}><div style={{fontSize:11,color:"var(--muted)",letterSpacing:".04em",textTransform:"uppercase",fontWeight:600}}>{l}</div><div className="num" style={{fontSize:13,fontWeight:600,marginTop:2}}>{v}</div></div>
                  ))}
                </div>
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",marginBottom:8}}>
                  <div className="eyebrow">Stock history</div>
                  <button className="btn btn-ghost btn-sm">View all</button>
                </div>
                {[
                  {t:"Sold · S-00010",q:-1,d:"26 May",bal:14},
                  {t:"Sold · S-00005",q:-2,d:"26 May",bal:15},
                  {t:"Received · PO-1042",q:+10,d:"26 May",bal:17},
                  {t:"Adjustment · stocktake",q:-1,d:"24 May",bal:7},
                  {t:"Sold · S-09920",q:-3,d:"22 May",bal:8},
                ].map((h,i)=>(
                  <div key={i} style={{display:"flex",alignItems:"center",gap:10,padding:"8px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                    <span style={{width:8,height:8,borderRadius:99,background:h.q>0?"var(--emerald)":"var(--crimson)"}}/>
                    <div style={{flex:1}}><div style={{fontSize:13,fontWeight:600}}>{h.t}</div></div>
                    <span className="num" style={{fontWeight:700,color:h.q>0?"var(--emerald)":"var(--crimson)"}}>{h.q>0?"+":""}{h.q}</span>
                    <span className="num" style={{fontSize:12,color:"var(--muted)",width:50,textAlign:"right"}}>{h.bal}</span>
                    <span className="num" style={{fontSize:11,color:"var(--muted)",width:60}}>{h.d}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
function StatMini({eyebrow,v,delta}){return(<div className="card-machined" style={{padding:12}}><div className="eyebrow" style={{fontSize:9}}>{eyebrow}</div><div className="num" style={{fontSize:18,fontWeight:800,marginTop:2}}>{v}</div><div className="num" style={{fontSize:11,color:"var(--emerald)",fontWeight:600,marginTop:2}}>{delta}</div></div>);}

/* ============================================================
   Sales return
   ============================================================ */
function SalesReturn({ theme, onTheme, onBack }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Sales Return" subtitle="Return items from invoice S-00010"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-ghost btn-sm" onClick={onBack}>Cancel</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{maxWidth:800,margin:"0 auto",display:"flex",flexDirection:"column",gap:14}}>
            <div className="card" style={{padding:"14px 18px",borderColor:"var(--amber-soft)",background:"var(--amber-tint)",display:"flex",gap:10,alignItems:"flex-start"}}>
              <Icon.bell size={16} style={{color:"var(--amber-press)",marginTop:2,flexShrink:0}}/>
              <div style={{fontSize:13,color:"var(--graphite)",lineHeight:1.4}}>Select items to return. Refund will be credited back to the original payment method or store credit.</div>
            </div>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow" style={{marginBottom:10}}>Original invoice · S-00010</div>
              <div style={{fontSize:13,color:"var(--muted)"}}>Ravi Soobramoney · 26 May 14:08 · paid cash</div>
              <div style={{marginTop:14}}>
                {[
                  {n:"T-Type Wrench 17mm",k:"wrench",q:2,p:185,ret:false},
                  {n:"Hammer Claw 16oz Forged",k:"hammer",q:1,p:320,ret:true},
                  {n:"Sprayer 20L Hi-Pressure",k:"sprayer",q:1,p:1250,ret:false},
                ].map((l,i)=>(
                  <div key={i} style={{display:"flex",alignItems:"center",gap:12,padding:"12px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                    <input type="checkbox" defaultChecked={l.ret} style={{accentColor:"var(--amber)",width:18,height:18}}/>
                    <div style={{width:40,height:40,borderRadius:8,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}><ProductTile kind={l.k} size={36}/></div>
                    <div style={{flex:1}}>
                      <div style={{fontSize:13,fontWeight:600}}>{l.n}</div>
                      <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{l.q} × Rs {l.p.toLocaleString()}</div>
                    </div>
                    <div style={{display:"flex",alignItems:"center",gap:8}}>
                      <div className="label" style={{margin:0}}>Return qty</div>
                      <div style={{display:"flex",alignItems:"center",border:"1px solid var(--hairline)",borderRadius:8,overflow:"hidden",background:"var(--raised)"}}>
                        <button style={{width:30,height:32,border:0,background:"transparent",cursor:"pointer"}}><Icon.minus size={14}/></button>
                        <span className="num" style={{width:28,textAlign:"center",fontWeight:700}}>{l.ret?1:0}</span>
                        <button style={{width:30,height:32,border:0,background:"transparent",cursor:"pointer"}}><Icon.plus size={14}/></button>
                      </div>
                    </div>
                    <div className="num" style={{width:90,textAlign:"right",fontWeight:700}}>{l.ret?`Rs ${l.p.toLocaleString()}`:"—"}</div>
                  </div>
                ))}
              </div>
            </div>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow" style={{marginBottom:10}}>Refund method</div>
              <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:10}}>
                {[{l:"Cash refund",ic:"cash",on:true},{l:"Store credit",ic:"wallet",on:false},{l:"Original method",ic:"card",on:false}].map((m,i)=>{const IcC=Icon[m.ic]; return (
                  <button key={i} style={{padding:"14px",borderRadius:12,border:m.on?"1.5px solid var(--ink)":"1px solid var(--hairline)",background:m.on?"var(--raised)":"var(--raised-2)",cursor:"pointer",display:"flex",flexDirection:"column",alignItems:"center",gap:6,color:"var(--ink)"}}>
                    <IcC size={20}/><span style={{fontSize:13,fontWeight:700}}>{m.l}</span>
                  </button>
                )})}
              </div>
              <div style={{marginTop:14}}><div className="label">Reason for return</div><div className="field"><input defaultValue="Defective — handle cracked"/></div></div>
            </div>
            <div style={{display:"flex",justifyContent:"space-between",alignItems:"center"}}>
              <div>
                <div style={{fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600}}>Refund total</div>
                <div className="num" style={{fontSize:28,fontWeight:800,letterSpacing:"-0.02em"}}>Rs 320</div>
              </div>
              <div style={{display:"flex",gap:8}}>
                <button className="btn btn-secondary" onClick={onBack}>Cancel</button>
                <button className="btn btn-primary btn-lg"><Icon.refresh size={16}/> Process return · Rs 320</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Low stock alerts
   ============================================================ */
function LowStock({ theme, onTheme, onBack }) {
  const items = PRODUCTS.filter(p=>p.stock<=10);
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="products"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Low Stock Alerts" subtitle="7 items below threshold"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Products</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.truck size={14}/> Quick re-order</button>
          </div>}/>
        <div className="scroll reveal" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:14}}>
            {items.map(p=>(
              <div key={p.id} className="card-machined" style={{padding:18,borderLeft:"3px solid var(--low)"}}>
                <div style={{display:"flex",alignItems:"flex-start",gap:12}}>
                  <div style={{width:54,height:54,borderRadius:10,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center",border:"1px solid var(--hairline-2)"}}><ProductTile kind={p.kind} size={48}/></div>
                  <div style={{flex:1}}>
                    <div style={{fontSize:14,fontWeight:700}}>{p.name}</div>
                    <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{p.sku}</div>
                  </div>
                </div>
                <div style={{display:"grid",gridTemplateColumns:"1fr 1fr 1fr",gap:8,marginTop:14}}>
                  <div><div style={{fontSize:10,color:"var(--muted)",textTransform:"uppercase",letterSpacing:".06em",fontWeight:600}}>In stock</div><div className="num" style={{fontSize:20,fontWeight:800,color:"var(--low)"}}>{p.stock}</div></div>
                  <div><div style={{fontSize:10,color:"var(--muted)",textTransform:"uppercase",letterSpacing:".06em",fontWeight:600}}>Threshold</div><div className="num" style={{fontSize:16,fontWeight:700}}>5</div></div>
                  <div><div style={{fontSize:10,color:"var(--muted)",textTransform:"uppercase",letterSpacing:".06em",fontWeight:600}}>Re-order</div><div className="num" style={{fontSize:16,fontWeight:700}}>20</div></div>
                </div>
                <button className="btn btn-secondary btn-sm" style={{width:"100%",marginTop:12}}><Icon.truck size={14}/> Re-order from supplier</button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Staff & Roles
   ============================================================ */
function StaffRoles({ theme, onTheme, onBack }) {
  const staff = [
    {name:"Sameer Khan",role:"Owner",email:"sameer@rbtrading.mu",init:"SK",c:"var(--amber)",perm:"Full access"},
    {name:"Shaad Khan",role:"Manager",email:"shaad@rbtrading.mu",init:"SK",c:"var(--ink)",perm:"All except delete"},
    {name:"Ritu Doorgakant",role:"Cashier",email:"ritu@rbtrading.mu",init:"RD",c:"var(--emerald)",perm:"POS · view products"},
    {name:"Deven Ramchurn",role:"Cashier",email:"deven@rbtrading.mu",init:"DR",c:"var(--graphite)",perm:"POS · view products"},
  ];
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="settings"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Staff & Roles" subtitle="4 active users · 3 roles"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Settings</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.plus size={14}/> Add user</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{maxWidth:900,margin:"0 auto",display:"flex",flexDirection:"column",gap:14}}>
            <div className="card" style={{padding:0,overflow:"hidden"}}>
              {staff.map((s,i)=>(
                <div key={i} style={{display:"flex",alignItems:"center",gap:14,padding:"16px 20px",borderBottom:i<staff.length-1?"1px solid var(--hairline-2)":"none"}}>
                  <div style={{width:46,height:46,borderRadius:12,background:s.c,color:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"var(--mono)",fontWeight:700,fontSize:14}}>{s.init}</div>
                  <div style={{flex:1}}>
                    <div style={{fontSize:15,fontWeight:700}}>{s.name}</div>
                    <div style={{fontSize:12,color:"var(--muted)",fontFamily:"var(--mono)"}}>{s.email}</div>
                  </div>
                  <span className={"badge "+(s.role==="Owner"?"badge-amber":s.role==="Manager"?"badge-paid":"badge-ghost")}>{s.role}</span>
                  <div style={{fontSize:12,color:"var(--muted)",maxWidth:180}}>{s.perm}</div>
                  <button className="btn btn-ghost btn-sm"><Icon.setting size={14}/></button>
                </div>
              ))}
            </div>
            <div className="card-machined" style={{padding:18}}>
              <div className="eyebrow" style={{marginBottom:10}}>Roles</div>
              <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:12}}>
                {[{r:"Owner",d:"Full access to all features",n:1},{r:"Manager",d:"Everything except deleting business data",n:1},{r:"Cashier",d:"POS · view products · view own sales",n:2}].map((ro,i)=>(
                  <div key={i} className="card" style={{padding:14,boxShadow:"none"}}>
                    <div style={{fontSize:15,fontWeight:700}}>{ro.r}</div>
                    <div style={{fontSize:12,color:"var(--muted)",marginTop:2}}>{ro.d}</div>
                    <div className="num" style={{fontSize:12,color:"var(--graphite)",marginTop:6}}>{ro.n} user{ro.n>1?"s":""}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Business Profile edit
   ============================================================ */
function BusinessProfile({ theme, onTheme, onBack }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="settings"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Business Profile" subtitle="Edit your shop details"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Settings</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.check size={14}/> Save</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{maxWidth:760,margin:"0 auto",display:"flex",flexDirection:"column",gap:14}}>
            <div className="card-machined" style={{padding:20,display:"flex",alignItems:"center",gap:18}}>
              <div style={{width:90,height:90,borderRadius:18,background:"var(--ink)",color:"var(--amber)",display:"flex",alignItems:"center",justifyContent:"center"}}><Icon.logo size={48}/></div>
              <div style={{flex:1}}>
                <div style={{fontSize:20,fontWeight:800}}>QUINCAILLERIE RB TRADING</div>
                <div className="num" style={{fontSize:12,color:"var(--muted)"}}>BRN C20177445 · VAT20188822</div>
                <button className="btn btn-secondary btn-sm" style={{marginTop:8}}>Change logo</button>
              </div>
            </div>
            <div className="card-machined" style={{padding:20}}>
              <div className="eyebrow" style={{marginBottom:14}}>Business information</div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                <div style={{gridColumn:"span 2"}}><div className="label">Business name</div><div className="field"><input defaultValue="QUINCAILLERIE RB TRADING"/></div></div>
                <div><div className="label">BRN</div><div className="field"><input className="num" defaultValue="C20177445"/></div></div>
                <div><div className="label">VAT number</div><div className="field"><input className="num" defaultValue="VAT20188822"/></div></div>
                <div style={{gridColumn:"span 2"}}><div className="label">Address</div><div className="field"><input defaultValue="Royal Rd, Curepipe, Mauritius"/></div></div>
                <div><div className="label">Phone</div><div className="field"><input className="num" defaultValue="+230 670 4408"/></div></div>
                <div><div className="label">Email</div><div className="field"><input defaultValue="info@rbtrading.mu"/></div></div>
              </div>
            </div>
            <div className="card-machined" style={{padding:20}}>
              <div className="eyebrow" style={{marginBottom:14}}>Invoice & receipt</div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                <div><div className="label">Invoice prefix</div><div className="field"><input className="num" defaultValue="S-"/></div></div>
                <div><div className="label">Next number</div><div className="field"><input className="num" defaultValue="00011"/></div></div>
                <div><div className="label">Footer text</div><div className="field"><input defaultValue="Goods sold are not refundable."/></div></div>
                <div><div className="label">Currency</div><div className="field"><input defaultValue="Rs · MUR"/></div></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Global search overlay
   ============================================================ */
function SearchOverlay({ onClose, onGoTo }) {
  return (
    <div style={{position:"absolute",inset:0,zIndex:50,background:"rgba(20,17,12,0.6)",display:"flex",alignItems:"flex-start",justifyContent:"center",paddingTop:60}} onClick={onClose}>
      <div className="card-machined" style={{width:640,maxHeight:"70%",display:"flex",flexDirection:"column",overflow:"hidden"}} onClick={e=>e.stopPropagation()}>
        <div style={{padding:"14px 18px",borderBottom:"1px solid var(--hairline)",display:"flex",gap:10,alignItems:"center"}}>
          <Icon.search size={18}/>
          <input autoFocus placeholder="Search products, invoices, customers…" style={{flex:1,border:0,outline:0,background:"transparent",fontSize:16,fontFamily:"var(--display)",color:"var(--ink)"}}/>
          <span style={{fontFamily:"var(--mono)",fontSize:10,background:"var(--raised-2)",padding:"3px 8px",borderRadius:4,border:"1px solid var(--hairline)",color:"var(--muted)"}}>ESC</span>
        </div>
        <div className="scroll" style={{overflow:"auto",padding:8}}>
          <div className="eyebrow" style={{padding:"8px 10px"}}>Products</div>
          {PRODUCTS.slice(0,4).map(p=>(
            <button key={p.id} onClick={()=>{onGoTo&&onGoTo("product-detail");onClose()}} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 10px",width:"100%",background:"transparent",border:0,borderRadius:8,cursor:"pointer",color:"var(--ink)",textAlign:"left"}}
              onMouseEnter={e=>e.currentTarget.style.background="var(--raised-2)"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
              <div style={{width:32,height:32,borderRadius:6,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}><ProductTile kind={p.kind} size={28}/></div>
              <div style={{flex:1}}><div style={{fontSize:13,fontWeight:600}}>{p.name}</div><div className="num" style={{fontSize:11,color:"var(--muted)"}}>{p.sku}</div></div>
              <span className="num" style={{fontSize:13,fontWeight:700}}>Rs {p.price.toLocaleString()}</span>
            </button>
          ))}
          <div className="eyebrow" style={{padding:"8px 10px",marginTop:6}}>Customers</div>
          {["Ravi Soobramoney","Chemtex Co. Ltd","D. Sundoo Hardware"].map((n,i)=>(
            <button key={i} onClick={()=>{onGoTo&&onGoTo("parties");onClose()}} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 10px",width:"100%",background:"transparent",border:0,borderRadius:8,cursor:"pointer",color:"var(--ink)",textAlign:"left"}}
              onMouseEnter={e=>e.currentTarget.style.background="var(--raised-2)"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
              <div style={{width:32,height:32,borderRadius:99,background:"var(--raised-2)",display:"flex",alignItems:"center",justifyContent:"center",fontFamily:"var(--mono)",fontWeight:700,fontSize:11,color:"var(--graphite)"}}>{n.split(" ").map(x=>x[0]).slice(0,2).join("")}</div>
              <div style={{fontSize:13,fontWeight:600}}>{n}</div>
            </button>
          ))}
          <div className="eyebrow" style={{padding:"8px 10px",marginTop:6}}>Invoices</div>
          {["S-00010 · Rs 2,145","S-00009 · Rs 3,240","S-00008 · Rs 18,420"].map((n,i)=>(
            <button key={i} onClick={()=>{onGoTo&&onGoTo("invoice-detail");onClose()}} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 10px",width:"100%",background:"transparent",border:0,borderRadius:8,cursor:"pointer",color:"var(--ink)",textAlign:"left"}}
              onMouseEnter={e=>e.currentTarget.style.background="var(--raised-2)"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
              <div style={{width:32,height:32,borderRadius:8,background:"var(--raised-2)",display:"flex",alignItems:"center",justifyContent:"center"}}><Icon.receipt size={14}/></div>
              <div className="num" style={{fontSize:13,fontWeight:600}}>{n}</div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { InvoiceDetail, ProductDetail, SalesReturn, LowStock, StaffRoles, BusinessProfile, SearchOverlay });
