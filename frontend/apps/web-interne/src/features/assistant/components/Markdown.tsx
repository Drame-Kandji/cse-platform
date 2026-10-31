import { Fragment } from 'react'
import { parseMarkdownBlocks, renderInline } from '../lib/markdown'

/**
 * Affiche le markdown simple des reponses de l'assistant sans `dangerouslySetInnerHTML`.
 * @example <Markdown source={reponse.reponse} />
 */
export function Markdown({ source }: { source: string }) {
  return (
    <div className="grid gap-2 text-sm leading-relaxed">
      {parseMarkdownBlocks(source).map((block, index) => {
        if (block.kind === 'heading') {
          return (
            <p key={index} className="font-semibold text-foreground">
              {renderInline(block.text)}
            </p>
          )
        }
        if (block.kind === 'list') {
          const List = block.ordered ? 'ol' : 'ul'
          return (
            <List
              key={index}
              className={block.ordered ? 'grid list-decimal gap-1 pl-5' : 'grid list-disc gap-1 pl-5'}
            >
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInline(item)}</li>
              ))}
            </List>
          )
        }
        return (
          <p key={index}>
            {block.lines.map((line, lineIndex) => (
              <Fragment key={lineIndex}>
                {lineIndex > 0 && <br />}
                {renderInline(line)}
              </Fragment>
            ))}
          </p>
        )
      })}
    </div>
  )
}
