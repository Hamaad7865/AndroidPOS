/* purchases.jsx — Purchase list, Add Purchase, Purchase detail */
const { useState: upuS } = React;

const PURCHASES = [
  {no:"PO-1042", supplier:"Ducray Hardware Ltd",  d:"26 May 09:30", items:8,  amt:42600, status:"received", pay:"credit"},
  {no:"PO-1041", supplier:"Brico Depot Maurice",  d:"24 May 14:12", items:14, amt:68200, status:"received", pay:"bank"},
  {no:"PO-1040", supplier:"Toolmax Intl",          d:"22 May 11:40", items:5,  amt:28950, status:"partial",  pay:"credit"},
  {no:"PO-1039", supplier:"M.K. Plumbing Supply", d:"20 May 16:05", items:22, amt:31400, status:"pending",  pay:"cash"},
  {no:"PO-1038", supplier:"Ducray Hardware Ltd",  d:"18 May 10:20", items:10, amt:54800, status:"received", pay:"bank"},
  {no:"PO-1037", supplier:"Paint World Ltée",      d:"15 May 09:48", items:18, amt:72100, status:"received", pay:"credit"},
  {no:"PO-1036", supplier:"Brico Depot Maurice",  d:"12 May 14:30", items:6,  amt:19500, status:"received", pay:"bank"},
  {no:"PO-1035", supplier:"Fastener Hub Co.",      d:"10 May 11:18", items:40, amt:8200,  status:"received", pay:"cash"},
];

function PurchaseList({ theme, onTheme, onGoTo }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="purchase"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Purchases" subtitle="42 orders this month · Rs 325.75K"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm"><Icon.filter size={14}/> This month</button>
            <button className="btn btn-secondary btn-sm"><Icon.download size={14}/> Export</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm" onClick={()=>onGoTo&&onGoTo("add-purchase")}><Icon.plus size={14}/> New purchase</button>
          </div>}/>

        <div style={{padding:"14px 22px 0",display:"flex",gap:10,alignItems:"center"}}>
          <div className="field" style={{flex:1,maxWidth:380}}><Icon.search size={16}/><input placeholder="Search PO number, supplier…"/></div>
          <div style={{display:"flex",gap:6}}>
            {["All","Received","Partial","Pending","Cancelled"].map((s,i)=>(
              <button key={s} className={"chip"+(i===0?" active":"")}>{s}</button>
            ))}
          </div>
        </div>

        <div style={{padding:"14px 22px 22px",flex:1,overflow:"hidden"}}>
          <div className="card" style={{height:"100%",display:"flex",flexDirection:"column",overflow:"hidden"}}>
            <div style={puTh}>
              <div style={{width:110}}>PO No.</div>
              <div style={{flex:1}}>Supplier</div>
              <div style={{width:160}}>Date</div>
              <div style={{width:80,textAlign:"right"}}>Items</div>
              <div style={{width:130,textAlign:"right"}}>Amount</div>
              <div style={{width:100}}>Payment</div>
              <div style={{width:100}}>Status</div>
              <div style={{width:32}}></div>
            </div>
            <div className="scroll reveal" style={{flex:1,overflow:"auto"}}>
              {PURCHASES.map(p=>(
                <div key={p.no} style={puTr} onClick={()=>onGoTo&&onGoTo("purchase-detail")}>
                  <div className="num" style={{width:110,fontWeight:700,fontSize:13.5}}>{p.no}</div>
                  <div style={{flex:1,fontSize:13.5,fontWeight:600}}>{p.supplier}</div>
                  <div className="num" style={{width:160,fontSize:12,color:"var(--muted)"}}>{p.d}</div>
                  <div className="num" style={{width:80,textAlign:"right",fontSize:13}}>{p.items}</div>
                  <div className="num" style={{width:130,textAlign:"right",fontSize:15,fontWeight:700}}>Rs {p.amt.toLocaleString()}</div>
                  <div style={{width:100,fontSize:12,color:"var(--graphite)",textTransform:"capitalize"}}>{p.pay}</div>
                  <div style={{width:100}}>
                    <span className={"badge "+(p.status==="received"?"badge-paid":p.status==="partial"?"badge-amber":"badge-due")}>
                      {p.status}
                    </span>
                  </div>
                  <button style={{width:32,border:0,background:"transparent",cursor:"pointer",color:"var(--muted)"}}><Icon.chev_r size={16}/></button>
                </div>
              ))}
            </div>
            <div style={{padding:"10px 16px",borderTop:"1px solid var(--hairline)",background:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"space-between"}}>
              <div style={{fontSize:12,color:"var(--muted)"}}>Showing <span className="num" style={{color:"var(--ink)",fontWeight:600}}>8</span> of <span className="num" style={{color:"var(--ink)",fontWeight:600}}>42</span></div>
              <div style={{display:"flex",gap:6}}>
                <button className="btn btn-secondary btn-sm" style={{background:"var(--ink)",color:"var(--surface)"}}>1</button>
                <button className="btn btn-secondary btn-sm">2</button>
                <button className="btn btn-secondary btn-sm">3</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
const puTh = {display:"flex",gap:16,alignItems:"center",padding:"12px 18px",borderBottom:"1px solid var(--hairline)",fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600,background:"var(--surface)"};
const puTr = {display:"flex",gap:16,alignItems:"center",padding:"12px 18px",borderBottom:"1px solid var(--hairline-2)",cursor:"pointer"};

/* ---------- Add Purchase ---------- */
function AddPurchase({ theme, onTheme, onBack }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="purchase"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="New Purchase Order" subtitle="Select supplier, add items, confirm"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-ghost btn-sm" onClick={onBack}>Cancel</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.check size={14}/> Confirm PO</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{display:"grid",gridTemplateColumns:"1fr 380px",gap:18,maxWidth:1200}}>
            {/* Left — form */}
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              <div className="card-machined" style={{padding:20}}>
                <div className="eyebrow" style={{marginBottom:14}}>Supplier</div>
                <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                  <div style={{gridColumn:"span 2"}}><div className="label">Supplier name</div><div className="field"><Icon.search size={16}/><input defaultValue="Ducray Hardware Ltd"/></div></div>
                  <div><div className="label">Contact</div><div className="field"><input className="num" defaultValue="+230 466 8800"/></div></div>
                  <div><div className="label">Credit terms</div><div className="field"><input defaultValue="Net 30"/></div></div>
                </div>
              </div>

              <div className="card-machined" style={{padding:20}}>
                <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",marginBottom:14}}>
                  <div className="eyebrow">Items</div>
                  <button className="btn btn-secondary btn-sm"><Icon.plus size={14}/> Add item</button>
                </div>
                <div style={{borderRadius:10,border:"1px solid var(--hairline)",overflow:"hidden"}}>
                  <div style={{display:"flex",gap:12,padding:"10px 14px",background:"var(--surface)",borderBottom:"1px solid var(--hairline)",fontSize:11,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)",fontWeight:600}}>
                    <div style={{flex:2}}>Product</div>
                    <div style={{width:80,textAlign:"right"}}>Qty</div>
                    <div style={{width:100,textAlign:"right"}}>Cost/unit</div>
                    <div style={{width:110,textAlign:"right"}}>Subtotal</div>
                    <div style={{width:32}}></div>
                  </div>
                  {[
                    {n:"Sprayer 20L Hi-Pressure",k:"sprayer",q:10,c:850},
                    {n:"Viseuse Cordless 18V",k:"drill",q:6,c:985},
                    {n:"PVC Pipe 110mm × 3m",k:"pipe",q:20,c:320},
                    {n:"Paint Enamel 5L Brick",k:"paint",q:12,c:780},
                    {n:"T-Type Wrench 17mm",k:"wrench",q:24,c:125},
                  ].map((it,i)=>(
                    <div key={i} style={{display:"flex",gap:12,alignItems:"center",padding:"10px 14px",borderBottom:"1px solid var(--hairline-2)"}}>
                      <div style={{flex:2,display:"flex",alignItems:"center",gap:8}}>
                        <div style={{width:34,height:34,borderRadius:7,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}><ProductTile kind={it.k} size={30}/></div>
                        <span style={{fontSize:13,fontWeight:600}}>{it.n}</span>
                      </div>
                      <div className="num" style={{width:80,textAlign:"right",fontWeight:600}}>{it.q}</div>
                      <div className="num" style={{width:100,textAlign:"right"}}>Rs {it.c.toLocaleString()}</div>
                      <div className="num" style={{width:110,textAlign:"right",fontWeight:700}}>Rs {(it.q*it.c).toLocaleString()}</div>
                      <button style={{width:32,border:0,background:"transparent",cursor:"pointer",color:"var(--muted)"}}><Icon.trash size={14}/></button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="card-machined" style={{padding:20}}>
                <div className="eyebrow" style={{marginBottom:12}}>Notes & delivery</div>
                <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                  <div><div className="label">Expected delivery</div><div className="field"><input defaultValue="02 Jun 2026"/></div></div>
                  <div><div className="label">Payment method</div><div className="field"><select style={{flex:1,border:0,background:"transparent",fontFamily:"var(--display)",fontSize:14,color:"var(--ink)"}}><option>Bank transfer</option><option>Cash</option><option>Credit</option></select></div></div>
                  <div style={{gridColumn:"span 2"}}><div className="label">Internal notes</div><div className="field" style={{height:"auto",padding:10}}><textarea rows="2" style={{background:"transparent",border:0,outline:0,resize:"none",width:"100%",fontFamily:"var(--display)"}} defaultValue="Confirm batch numbers on delivery."/></div></div>
                </div>
              </div>
            </div>

            {/* Right — summary */}
            <div>
              <div className="card-machined" style={{padding:18,position:"sticky",top:0}}>
                <div className="eyebrow">Order summary</div>
                <div className="num" style={{fontSize:13,color:"var(--muted)",marginTop:6}}>PO-1043 · Draft</div>
                <div style={{marginTop:14,display:"flex",flexDirection:"column",gap:6}}>
                  <SumRow l="Items" v="5 products · 72 units"/>
                  <SumRow l="Subtotal" v="Rs 42,600" mono/>
                  <SumRow l="VAT (15%)" v="Rs 6,390" mono/>
                  <div style={{borderTop:"1px dashed var(--hairline)",margin:"6px 0"}}/>
                  <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline"}}>
                    <span style={{fontSize:13,letterSpacing:".06em",textTransform:"uppercase",fontWeight:600,color:"var(--muted)"}}>Total</span>
                    <span className="num" style={{fontSize:28,fontWeight:800,letterSpacing:"-0.02em"}}>Rs 48,990</span>
                  </div>
                </div>
                <div style={{display:"flex",gap:8,marginTop:18}}>
                  <button className="btn btn-secondary" style={{flex:1}}>Save draft</button>
                  <button className="btn btn-primary" style={{flex:1}}><Icon.check size={16}/> Confirm</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
function SumRow({l,v,mono}){ return (<div style={{display:"flex",justifyContent:"space-between",fontSize:13}}><span style={{color:"var(--muted)"}}>{l}</span><span className={mono?"num":""} style={{fontWeight:600}}>{v}</span></div>); }

/* ---------- Purchase Detail ---------- */
function PurchaseDetail({ theme, onTheme, onBack }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="purchase"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="PO-1042" subtitle="Ducray Hardware Ltd · 26 May 2026"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Back</button>
            <button className="btn btn-secondary btn-sm"><Icon.print size={14}/> Print</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{display:"grid",gridTemplateColumns:"1fr 340px",gap:18,maxWidth:1200}}>
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              {/* Status */}
              <div className="card-machined" style={{padding:18,display:"flex",alignItems:"center",gap:14}}>
                <div style={{width:50,height:50,borderRadius:12,background:"var(--emerald-soft)",color:"var(--emerald)",display:"flex",alignItems:"center",justifyContent:"center"}}><Icon.check size={22}/></div>
                <div style={{flex:1}}>
                  <div style={{fontSize:18,fontWeight:700}}>Received</div>
                  <div style={{fontSize:12,color:"var(--muted)",fontFamily:"var(--mono)"}}>All 8 items received · 26 May 09:30 · checked by S. Khan</div>
                </div>
                <span className="badge badge-paid">● Complete</span>
              </div>

              {/* Items table */}
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow" style={{marginBottom:10}}>Items received</div>
                {[
                  {n:"Sprayer 20L Hi-Pressure",k:"sprayer",q:10,c:850},
                  {n:"Viseuse Cordless 18V",k:"drill",q:6,c:985},
                  {n:"PVC Pipe 110mm × 3m",k:"pipe",q:20,c:320},
                  {n:"T-Type Wrench 17mm",k:"wrench",q:24,c:125},
                ].map((it,i)=>(
                  <div key={i} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 0",borderBottom:"1px solid var(--hairline-2)"}}>
                    <div style={{width:38,height:38,borderRadius:8,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}><ProductTile kind={it.k} size={34}/></div>
                    <div style={{flex:1}}>
                      <div style={{fontSize:13,fontWeight:600}}>{it.n}</div>
                      <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{it.q} × Rs {it.c.toLocaleString()}</div>
                    </div>
                    <div className="num" style={{fontSize:14,fontWeight:700}}>Rs {(it.q*it.c).toLocaleString()}</div>
                    <span className="badge badge-paid" style={{fontSize:10}}>✓ Rcvd</span>
                  </div>
                ))}
              </div>

              {/* Timeline */}
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow" style={{marginBottom:10}}>Timeline</div>
                {[
                  {t:"Items received & shelved",who:"S. Khan",d:"26 May 09:30",c:"var(--emerald)"},
                  {t:"Delivery arrived at shop",who:"Ducray driver",d:"26 May 09:15",c:"var(--ink)"},
                  {t:"PO confirmed & sent",who:"Sameer K.",d:"24 May 16:00",c:"var(--amber)"},
                  {t:"Draft created",who:"Sameer K.",d:"24 May 14:40",c:"var(--muted)"},
                ].map((ev,i)=>(
                  <div key={i} style={{display:"flex",gap:12,paddingBottom:14,position:"relative"}}>
                    <div style={{display:"flex",flexDirection:"column",alignItems:"center",gap:4,width:12}}>
                      <span style={{width:10,height:10,borderRadius:99,background:ev.c,flexShrink:0}}/>
                      {i<3 && <div style={{flex:1,width:1.5,background:"var(--hairline)"}}/>}
                    </div>
                    <div>
                      <div style={{fontSize:13,fontWeight:600}}>{ev.t}</div>
                      <div style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>{ev.who} · {ev.d}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Sidebar */}
            <div style={{display:"flex",flexDirection:"column",gap:14}}>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow">Supplier</div>
                <div style={{fontSize:16,fontWeight:700,marginTop:6}}>Ducray Hardware Ltd</div>
                <div className="num" style={{fontSize:12,color:"var(--muted)"}}>+230 466 8800 · Vacoas</div>
                <div className="num" style={{fontSize:12,color:"var(--muted)"}}>Credit terms · Net 30</div>
              </div>
              <div className="card-machined" style={{padding:18}}>
                <div className="eyebrow">Financials</div>
                <div style={{marginTop:8,display:"flex",flexDirection:"column",gap:4}}>
                  <SumRow l="Subtotal" v="Rs 42,600" mono/>
                  <SumRow l="VAT 15%" v="Rs 6,390" mono/>
                  <div style={{borderTop:"1px dashed var(--hairline)",margin:"4px 0"}}/>
                  <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline"}}>
                    <span style={{fontSize:11,letterSpacing:".06em",textTransform:"uppercase",fontWeight:600,color:"var(--muted)"}}>Total</span>
                    <span className="num" style={{fontSize:22,fontWeight:800}}>Rs 48,990</span>
                  </div>
                  <SumRow l="Payment" v="Bank transfer"/>
                  <SumRow l="Status" v="Paid"/>
                </div>
              </div>
              <div style={{display:"flex",gap:8}}>
                <button className="btn btn-secondary" style={{flex:1}}><Icon.print size={14}/> Print</button>
                <button className="btn btn-secondary" style={{flex:1}}><Icon.share size={14}/> Share</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { PurchaseList, AddPurchase, PurchaseDetail });
