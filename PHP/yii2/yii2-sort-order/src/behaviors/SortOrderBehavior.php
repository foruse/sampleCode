<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\sortorder\behaviors;

use Exception;
use yii\base\NotSupportedException;
use yii\behaviors\AttributeBehavior;
use yii\db\ActiveRecord;
use yii\db\BaseActiveRecord;
use yii\db\Expression;
use yii\db\Query;

/**
 * Class SortOrderBehavior
 */
class SortOrderBehavior extends AttributeBehavior
{
    /**
     * @var string
     */
    public $sortableAttribute = 'sort_order';

    /**
     * @var callable|Expression
     */
    public $value;

    /**
     * @var ActiveRecord
     */
    public $owner;

    /**
     * @var int
     */
    private $newPosition;

    /**
     * @inheritdoc
     */
    public function init()
    {
        parent::init();

        if (empty($this->attributes)) {
            $this->attributes = [
                BaseActiveRecord::EVENT_BEFORE_INSERT => $this->sortableAttribute,
                BaseActiveRecord::EVENT_BEFORE_UPDATE => $this->sortableAttribute,
            ];
        }
    }

    /**
     * @inheritdoc
     */
    protected function getValue($event)
    {
        if (!($this->owner instanceof ActiveRecord)) {
            throw new NotSupportedException('This behavior for models type of "ActiveRecord".');
        }
        if ($this->value instanceof Expression) {
            return $this->value;
        }

        $transaction = $this->owner->getDb()
            ->beginTransaction();
        try {

            $query = $this->makeUpdateQuery();
            $this->owner->getDb()
                ->createCommand($query)
                ->execute();

            $transaction->commit();
        } catch (Exception $exception) {
            $transaction->rollBack();
        }

        return $this->newPosition;
    }

    /**
     * @return array
     */
    protected function getSortedData()
    {
        return (new Query())->select(['id'])
            ->from($this->owner->tableName())
            ->where('id != :id', [':id' => (int) $this->owner->id])
            ->orderBy([$this->sortableAttribute => SORT_ASC])
            ->all($this->owner->getDb());
    }

    /**
     * @return array
     */
    private function getSortOrderData()
    {
        $result = [];
        $this->newPosition = (int) $this->owner->getAttribute($this->sortableAttribute);
        $oldPosition = (int) $this->owner->getOldAttribute($this->sortableAttribute);
        if ($oldPosition < $this->newPosition) {
            $this->newPosition -= 1;
        }

        $items = $this->getSortedData();
        $sortOrder = 0;
        foreach ($items as $item) {
            if ($this->newPosition === $sortOrder) {
                $sortOrder += 1;
            }
            $result[$item['id']] = $sortOrder;
            $sortOrder += 1;
        }

        return $result;
    }

    /**
     * @return string
     */
    private function makeUpdateQuery()
    {
        $sortOrderData = $this->getSortOrderData();

        if (empty($sortOrderData)) {
            return null;
        }

        $query = "UPDATE {$this->owner->tableName()} SET {$this->sortableAttribute} = CASE ";
        $ids = [];
        foreach ($sortOrderData as $id => $sortOrder) {
            $id = (int) $id;
            $sortOrder = (int) $sortOrder;
            $query .= "WHEN id = {$id} THEN {$sortOrder} ";
            $ids[] = $id;
        }
        $query .= " END WHERE id IN (" . implode(',', $ids) . ');';

        return $query;
    }
}
